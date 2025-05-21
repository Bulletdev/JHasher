package tools.jwt;

import java.util.Base64;
import java.util.Scanner;

public class JwtInspector {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o token JWT: ");
        String token = scanner.nextLine();

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            System.out.println("Token inválido (esperado 3 partes separadas por ponto)");
            return;
        }

        System.out.println("\nHeader:");
        System.out.println(new String(Base64.getUrlDecoder().decode(parts[0])));

        System.out.println("\nPayload:");
        System.out.println(new String(Base64.getUrlDecoder().decode(parts[1])));
    }
}
