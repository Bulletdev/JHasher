package com.bulletdev.jhasher.cli;

import com.bulletdev.jhasher.core.crypto.CryptoKeyGenerator;
import com.bulletdev.jhasher.core.output.OutputFormatter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.Base64;
import java.util.HexFormat;

@Command(name = "keygen", description = "Generate cryptographic keys")
public class KeygenCommand implements Runnable {

    @ParentCommand
    JHasherCli parent;

    @Option(names = "--size", defaultValue = "256", description = "Key size in bits (default: 256)")
    int bits;

    @Option(names = "--format", defaultValue = "hex", description = "Output format: hex, base64, raw (default: hex)")
    String format;

    @Option(names = "--password", description = "Generate a random password instead of a key")
    boolean generatePassword;

    @Option(names = "--length", defaultValue = "32", description = "Password length (default: 32)")
    int passwordLength;

    @Override
    public void run() {
        try {
            if (generatePassword) {
                String password = CryptoKeyGenerator.generatePassword(passwordLength);
                System.out.println(password);
                return;
            }

            byte[] key = CryptoKeyGenerator.generateKey(bits);
            String output = switch (format.toLowerCase()) {
                case "base64" -> Base64.getEncoder().encodeToString(key);
                case "raw" -> new String(key);
                default -> HexFormat.of().formatHex(key);
            };
            System.out.println(output);
        } catch (Exception e) {
            System.err.println(OutputFormatter.formatError(e.getMessage()));
        }
    }
}
