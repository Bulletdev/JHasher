package scanner;

import java.net.InetAddress;
import java.util.concurrent.*;

public class SubdomainScanner {
    public static void main(String[] args) throws Exception {
        System.out.println("== Subdomain Scanner ==");

        if (args.length < 1) {
            System.out.println("Uso: java SubdomainScanner dominio.com");
            return;
        }

        String domain = args[0];
        String[] wordlist = {"www", "mail", "ftp", "dev", "test", "api", "vpn", "ns1", "ns2"};
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (String sub : wordlist) {
            executor.submit(() -> {
                try {
                    String full = sub + "." + domain;
                    InetAddress.getByName(full);
                    System.out.println("[+] Encontrado: " + full);
                } catch (Exception ignored) {}
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }
}
