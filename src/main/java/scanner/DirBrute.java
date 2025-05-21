package scanner;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;

public class DirBrute {
    public static void main(String[] args) throws Exception {
        System.out.println("== Directory Bruteforcer ==");

        if (args.length < 1) {
            System.out.println("Uso: java DirBrute http://alvo.com/");
            return;
        }

        String target = args[0];
        String[] wordlist = {"admin", "login", "dashboard", "uploads", "images", "js", "css"};

        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (String dir : wordlist) {
            executor.submit(() -> {
                try {
                    URL url = new URL(target + (target.endsWith("/") ? "" : "/") + dir);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    if (conn.getResponseCode() < 400) {
                        System.out.println("[+] Encontrado: " + url + " (Status " + conn.getResponseCode() + ")");
                    }
                } catch (Exception ignored) {}
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }
}
