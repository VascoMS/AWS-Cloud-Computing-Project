package pt.ulisboa.tecnico.cnv;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import pt.ulisboa.tecnico.cnv.util.EMACalculator;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class AutoScaler implements Runnable, AutoscalerNotifier{
    private static final double SCALE_OUT_CPU_THRESHOLD = 0.85;
    private static final double SCALE_IN_CPU_THRESHOLD = 0.25;
    private static final int MIN_WORKERS = 1;
    private static final int MAX_WORKERS = 5;
    private static final String AWS_REGION = "us-east-1";
    private static final long OBS_TIME = 1000 * 60 * 5;
    private static final long POLL_INTERVAL_MS = 1000 * 60;
    private static final long SCALING_TIMEOUT = 1000 * 60 * 5;


    private final LoadBalancer loadBalancer;
    private final AmazonEC2 ec2;
    private final AmazonCloudWatch cloudWatch;
    private boolean running = true;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private long latestScalingTimestamp;


    public AutoScaler(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
        this.ec2 = AmazonEC2ClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).build();
        this.cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(AWS_REGION).withCredentials(new EnvironmentVariableCredentialsProvider()).build();
        // Create first worker here if needed
        initializeDefaultWorkers();
    }


    @Override
    public void run() {
        while (running) {
            long start = System.currentTimeMillis();
            autoscalerTick();
            long elapsed = System.currentTimeMillis() - start;
            lock.lock();
            try {
                long waitTime = POLL_INTERVAL_MS - elapsed;
                if (waitTime > 0) {
                    condition.wait(waitTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void wakeUp() {
        if (lock.tryLock()) {
            try {
                condition.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    public void initializeDefaultWorkers() {
        for (int i = 0; i < 3; i++) {
            this.loadBalancer.addNewWorker("w" + i, "localhost", 8000 + i + 1);
        }
    }
    private Set<Instance> getInstances(AmazonEC2 ec2) {
        Set<Instance> instances = new HashSet<>();
        for (Reservation reservation : ec2.describeInstances().getReservations()) {
            instances.addAll(reservation.getInstances());
        }
        return instances;
    }


    public void autoscalerTick() {
        double avgCpuUsage = fetchAverageCpuUsage();
        int globalQueueLength = loadBalancer.getGlobalQueueLength();
        int nrActiveVms = loadBalancer.getNrActiveVms();
        if (new Date().getTime() - latestScalingTimestamp < SCALING_TIMEOUT) {
            return;
        }
        if (avgCpuUsage > SCALE_OUT_CPU_THRESHOLD || globalQueueLength > 0) {
            if (nrActiveVms < MAX_WORKERS) {
                scaleOut();
            }
        } else if (avgCpuUsage < SCALE_IN_CPU_THRESHOLD) {
            if (nrActiveVms - 1 >= MIN_WORKERS) {
                scaleIn();
            }
        }
    }


    private double fetchAverageCpuUsage() {
        try {
            Set<Instance> instances = getInstances(ec2);
            System.out.println("total instances = " + instances.size());

            Dimension instanceDimension = new Dimension();
            instanceDimension.setName("InstanceId");

            double sum = 0;

            for (Instance instance : instances) {
                String iid = instance.getInstanceId();
                String state = instance.getState().getName();
                if (state.equals("running")) {
                    System.out.println("running instance id = " + iid);
                    instanceDimension.setValue(iid);
                    GetMetricStatisticsRequest request = new GetMetricStatisticsRequest().withStartTime(new Date(new Date().getTime() - OBS_TIME))
                            .withNamespace("AWS/EC2")
                            .withPeriod(60)
                            .withMetricName("CPUUtilization")
                            .withStatistics("Average")
                            .withDimensions(instanceDimension)
                            .withEndTime(new Date());

                    List<Double> dps = cloudWatch.getMetricStatistics(request).getDatapoints().stream()
                            .map(Datapoint::getAverage).toList();
                    sum += EMACalculator.calculateEMA(dps); // Using the exponential moving average to give more importance to recent datapoints
                }
                else {
                    System.out.println("instance id = " + iid + " state = " + state);
                }
            }

            return sum / instances.size();
        } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
            return -1;
        }
    }

    private String createNewWorker() {
        latestScalingTimestamp = new Date().getTime();
        return "vm-id-placeholder";
    }

    private void scaleOut() {
        String newWorkerId = createNewWorker();
        // TODO: Register new worker with LoadBalancer if needed
        System.out.println("Scaled out: started worker " + newWorkerId);
    }

    private void scaleIn() {
        String vmId = loadBalancer.getLeastLoadedWorker();
        if (vmId != null) {
            loadBalancer.initiateWorkerRemoval(vmId);
            System.out.println("Scaled in: removing worker " + vmId);
        }
    }
}
