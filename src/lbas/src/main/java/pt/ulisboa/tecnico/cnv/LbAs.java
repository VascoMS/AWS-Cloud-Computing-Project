package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class LbAs {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        LoadBalancer loadBalancer = new LoadBalancer();
        AutoScaler autoScaler = new AutoScaler(loadBalancer);
        server.createContext("/", loadBalancer.getNewRequestAssigner());
        server.start();
        System.out.println("Web server started on port 8000!");
    }
}