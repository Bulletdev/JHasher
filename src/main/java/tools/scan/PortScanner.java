package tools.scan;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PortScanner {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int timeout = 100;
        ExecutorService executor = Executors.newFixedThreadPool(100);

        for (int port = 1; port <= 1024; port++) {
            final int p = port;
            executor.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new java.net.InetSocketAddress(host, p), timeout);
                    System.out.println("Porta aberta: " + p);
                } catch (Exception ignored) {}
            });
        }

        executor.shutdown();
    }
}
