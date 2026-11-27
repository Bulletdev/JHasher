package com.bulletdev.jhasher.cli;

import com.bulletdev.jhasher.core.crypto.SymmetricCrypto;
import com.bulletdev.jhasher.core.output.OutputFormatter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Command(name = "decrypt", description = "Decrypt data")
public class DecryptCommand implements Runnable {

    @ParentCommand
    JHasherCli parent;

    @Parameters(index = "0", description = "Algorithm (e.g. aes-256-gcm, chacha20-poly1305)")
    String algorithm;

    @Parameters(index = "1", arity = "0..1", description = "Base64-encoded ciphertext")
    String input;

    @Option(names = "--key", required = true, description = "Decryption key/password")
    String key;

    @Option(names = "--iv", description = "Initialization vector (hex)")
    String iv;

    @Override
    public void run() {
        String data = resolveInput();
        if (data == null) {
            System.err.println(OutputFormatter.formatError("No input provided."));
            return;
        }

        try {
            String result = SymmetricCrypto.decryptFromBase64(algorithm, data, key);
            System.out.println(result);
        } catch (Exception e) {
            System.err.println(OutputFormatter.formatError(e.getMessage()));
        }
    }

    private String resolveInput() {
        if (input != null) {
            return input;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
