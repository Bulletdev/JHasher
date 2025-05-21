package tools.hashid;

public class HashIdentifier {
    public static void main(String[] args) {
        String hash = "5f4dcc3b5aa765d61d8327deb882cf99";

        if (hash.matches("^[a-fA-F0-9]{32}$")) {
            System.out.println("Possível MD5");
        } else if (hash.matches("^[a-fA-F0-9]{40}$")) {
            System.out.println("Possível SHA1");
        } else if (hash.matches("^[a-fA-F0-9]{64}$")) {
            System.out.println("Possível SHA256");
        } else if (hash.startsWith("$2a$") || hash.startsWith("$2b$")) {
            System.out.println("Possível BCrypt");
        } else {
            System.out.println("Tipo de hash desconhecido");
        }
    }
}
