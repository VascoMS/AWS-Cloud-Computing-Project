package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import lombok.Getter;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

@Getter
public class QueuedRequest {
    private final HttpExchange exchange;
    private final long estimatedComplexity;
    private final CompletableFuture<WorkerResponse> future;

    public QueuedRequest(HttpExchange exchange, long estimatedComplexity) {
        this.exchange = exchange;
        this.estimatedComplexity = estimatedComplexity;
        this.future = new CompletableFuture<>();
    }

}