package pt.ulisboa.tecnico.cnv;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthChecker {
    private final ConcurrentMap<String, Worker> workers;
    private final ScheduledExecutorService scheduler;

    public HealthChecker(ConcurrentMap<String, Worker> workers) {
        this.workers = workers;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    private void performHealthChecks() {
        workers.values().parallelStream().forEach(this::checkWorkerHealth);
    }

    private void checkWorkerHealth(Worker worker) {
        try {
            String healthUrl = "http://" + worker.getHost() + ":" + worker.getPort() + "/test";
            HttpURLConnection connection = (HttpURLConnection) new URL(healthUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            boolean isHealthy = responseCode >= 200 && responseCode < 300;

            if (!isHealthy && worker.isAvailable()) {
                System.err.println("Worker " + worker.getId() + " failed health check, marking unavailable");
                worker.setUnhealthy();
            } else if (isHealthy && worker.isUnhealthy()) {
                System.out.println("Worker " + worker.getId() + " recovered, marking available");
                worker.setAvailable();
            }

        } catch (IOException e) {
            if (worker.isAvailable()) {
                System.err.println("Worker " + worker.getId() + " health check failed: " + e.getMessage());
                worker.setUnhealthy();
            }
        }
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::performHealthChecks, 0, 30, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
