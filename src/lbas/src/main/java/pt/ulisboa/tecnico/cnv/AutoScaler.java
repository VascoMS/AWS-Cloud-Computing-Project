package pt.ulisboa.tecnico.cnv;

import java.util.Map;

public class AutoScaler {
    private static final double SCALE_OUT_CPU_THRESHOLD = 0.9;
    private static final double SCALE_IN_CPU_THRESHOLD = 0.25;
    private static final int MIN_WORKERS = 1;
    private static final int MAX_WORKERS = 10;

    private final LoadBalancer loadBalancer;

    public AutoScaler(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
        // Create first worker here if needed
        initializeDefaultWorkers();
    }

    public void initializeDefaultWorkers() {
        for (int i = 0; i < 3; i++) {
            this.loadBalancer.addNewWorker("w" + i, "localhost", 8000 + i + 1);
        }
    }

    public void autoscalerTick() {
        double avgCpuUsage = getAverageCpuUsage();
        int globalQueueLength = loadBalancer.getGlobalQueueLength();
        int nrActiveVms = loadBalancer.getNrActiveVms();

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

    private double getAverageCpuUsage() {
        // TODO: Implement actual logic
        return 0.0;
    }

    private String createNewWorker() {
        // TODO: Implement worker creation logic
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
