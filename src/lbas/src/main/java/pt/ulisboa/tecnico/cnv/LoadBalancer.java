package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import lombok.Getter;
import pt.ulisboa.tecnico.cnv.storage.StorageUtil;
import pt.ulisboa.tecnico.cnv.strategies.VmSelectionStrategy;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class LoadBalancer {
    public static final long VM_CAPACITY = 44153067L;
    public static final long LAMBDA_THRESHOLD = 756005L;
    public static final double SPREAD_THRESHOLD = 0.85;
    public static final double PACK_THRESHOLD = 0.25;

    private final ConcurrentMap<String, Worker> workers = new ConcurrentHashMap<>();
    private final Queue<QueuedRequest> globalOverflowQueue = new LinkedBlockingQueue<>();
    private final ComplexityEstimator complexityEstimator;
    private final LambdaInvoker lambdaInvoker;
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final ScheduledExecutorService queueProcessor;
    private final LoadBalancerMetrics metrics;
    private final HealthChecker healthChecker;

    public enum WorkerRemovalStatus {
        PENDING, REMOVED
    }

    public LoadBalancer() throws InterruptedException {
        this.complexityEstimator = new ComplexityEstimator();
        this.lambdaInvoker = new LambdaInvoker();
        this.queueProcessor = Executors.newScheduledThreadPool(2);
        this.metrics = new LoadBalancerMetrics();
        this.healthChecker = new HealthChecker(workers);

        StorageUtil.createTable();

        healthChecker.start();

        // Print metrics every 60 seconds
        //queueProcessor.scheduleAtFixedRate(metrics::printStats, 60, 60, TimeUnit.SECONDS);
    }

    public RequestAssigner getNewRequestAssigner() {
        return new RequestAssigner(this);
    }


    public String getLeastLoadedWorker(){
        return workers.values().stream().min(Comparator.comparing(Worker::getCurrentLoad)).get().getId();
    }

    public CompletableFuture<WorkerResponse> tryAssignToBestCandidate(
            HttpExchange exchange, RequestAssigner.RequestContext requestContext, VmSelectionStrategy strategy) {

        long requestComplexity = requestContext.complexity();
        List<String> candidateVmIds = strategy.selectVms(workers, requestComplexity, requestContext.avgLoad());

        for (String vmId : candidateVmIds) {
            Worker worker = workers.get(vmId);
            if (worker == null || !worker.isAvailable()) {
                System.out.println("VM " + vmId + " is not available");
                continue; // Skip non-existent or known unhealthy workers
            }

            if (worker.tryAssignLoad(requestComplexity)) {
                System.out.printf("Reserved load on VM %s for request with complexity %d.%n",
                        vmId, requestContext.complexity());

                CompletableFuture<WorkerResponse> future = HttpForwarder.forwardRequest(worker, exchange, requestContext.storeMetrics());

                future.whenComplete((response, throwable) -> {
                    worker.decreaseLoad(requestComplexity);
                    if (throwable != null) {
                        worker.setUnhealthy();
                    }
                });

                getMetrics().incrementForwardedToWorkers();
                return future;
            }
        }

        if(requestComplexity < LAMBDA_THRESHOLD) {
            return lambdaInvoker.invokeLambda(exchange.getRequestURI());
        }

        return queueRequest(exchange, requestComplexity);
    }

    public CompletableFuture<WorkerResponse> queueRequest(HttpExchange exchange, long complexity) {
        QueuedRequest request = new QueuedRequest(exchange, complexity);
        globalOverflowQueue.add(request);
        System.out.println("Queued request " + request);
        return request.getFuture();
    }


    // Method to calculate average load - called by RequestAssigner
    public long calculateAverageLoad() {
        long totalLoad = workers.values().stream()
                .filter(Worker::isAvailable)
                .mapToLong(Worker::getCurrentLoad)
                .sum();

        long availableCount = workers.values().stream()
                .filter(Worker::isAvailable)
                .count();

        return availableCount > 0 ?  totalLoad / availableCount : 0L;
    }


    // Worker management methods
    public void addNewWorker(String workerId, String host, int port) {
        Worker worker = new Worker(workerId, host, port);
        workers.put(workerId, worker);
    }

    public void initiateWorkerRemoval(String workerId) {
        Worker worker = workers.get(workerId);
        if (worker != null) {
            worker.setDraining();
        }
    }

    public WorkerRemovalStatus finalizeWorkerRemoval(String workerId) {
        Worker worker = workers.get(workerId);
        if (worker == null) return WorkerRemovalStatus.REMOVED;

        long load = worker.getCurrentLoad();
        if (load > 0) {
            return WorkerRemovalStatus.PENDING;
        }

        workers.remove(workerId);
        return WorkerRemovalStatus.REMOVED;
    }

    public int getGlobalQueueLength() {
        return globalOverflowQueue.size();
    }

    public int getNrActiveVms() {
        return workers.size();
    }

    public Set<String> getDrainingWorkers() {
        return workers.entrySet().stream()
                .filter(entry -> entry.getValue().isDraining())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }


    public Worker getWorker(String id){
        return workers.get(id);
    }

}