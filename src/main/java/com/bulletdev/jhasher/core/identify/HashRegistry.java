package com.bulletdev.jhasher.core.identify;

import com.bulletdev.jhasher.core.identify.algorithms.ApplicationHashes;
import com.bulletdev.jhasher.core.identify.algorithms.ChecksumHashes;
import com.bulletdev.jhasher.core.identify.algorithms.CryptographicHashes;
import com.bulletdev.jhasher.core.identify.algorithms.PasswordHashes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class HashRegistry {

    private static final HashRegistry INSTANCE = new HashRegistry();

    private final List<HashAlgorithm> algorithms = new ArrayList<>();

    private HashRegistry() {
        registerAll();
    }

    public static HashRegistry getInstance() {
        return INSTANCE;
    }

    public void register(HashAlgorithm algorithm) {
        algorithms.add(algorithm);
    }

    public List<HashAlgorithm> getAll() {
        return Collections.unmodifiableList(algorithms);
    }

    public Optional<HashAlgorithm> getById(String id) {
        return algorithms.stream()
                .filter(a -> a.id().equals(id))
                .findFirst();
    }

    public List<HashAlgorithm> getByCategory(String category) {
        return algorithms.stream()
                .filter(a -> a.category().equals(category))
                .toList();
    }

    private void registerAll() {
        // Cryptographic hashes
        register(new CryptographicHashes.Md5());
        register(new CryptographicHashes.Md4());
        register(new CryptographicHashes.Sha1());
        register(new CryptographicHashes.Sha224());
        register(new CryptographicHashes.Sha256());
        register(new CryptographicHashes.Sha384());
        register(new CryptographicHashes.Sha512());
        register(new CryptographicHashes.Sha3_256());
        register(new CryptographicHashes.Sha3_512());
        register(new CryptographicHashes.Ripemd160());
        register(new CryptographicHashes.Whirlpool());
        register(new CryptographicHashes.Blake2b256());
        register(new CryptographicHashes.Blake2b512());

        // Password hashes
        register(new PasswordHashes.BCryptHash());
        register(new PasswordHashes.Argon2Hash());
        register(new PasswordHashes.ScryptHash());
        register(new PasswordHashes.YescryptHash());
        register(new PasswordHashes.Pbkdf2Hash());
        register(new PasswordHashes.LinuxSha512Crypt());
        register(new PasswordHashes.LinuxSha256Crypt());
        register(new PasswordHashes.LinuxMd5Crypt());

        // Application hashes
        register(new ApplicationHashes.Ntlm());
        register(new ApplicationHashes.MySqlOld());
        register(new ApplicationHashes.MySql5());
        register(new ApplicationHashes.PostgresMd5());
        register(new ApplicationHashes.MsSql2005());
        register(new ApplicationHashes.MsSql2012());
        register(new ApplicationHashes.WordPress());
        register(new ApplicationHashes.Drupal());
        register(new ApplicationHashes.DjangoSha256());
        register(new ApplicationHashes.OracleH());

        // Checksum hashes
        register(new ChecksumHashes.Crc32());
        register(new ChecksumHashes.Adler32Hash());
        register(new ChecksumHashes.Crc16());
    }
}
