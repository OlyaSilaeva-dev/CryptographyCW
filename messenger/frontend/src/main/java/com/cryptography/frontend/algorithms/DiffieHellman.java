package com.cryptography.frontend.algorithms;

import java.math.BigInteger;
import java.security.SecureRandom;

public class DiffieHellman {
    private final BigInteger p;
    private final BigInteger g;
    private final BigInteger privateKey;
    private final BigInteger publicKey;

    public DiffieHellman(BigInteger p, BigInteger g) {
        this.p = p;
        this.g = g;
        this.privateKey = generatePrivateKey(p); // 1 < privateKey < p-1
        this.publicKey = g.modPow(privateKey, p);
    }

    private BigInteger generatePrivateKey(BigInteger p) {
        SecureRandom random = new SecureRandom();
        BigInteger max = p.subtract(BigInteger.valueOf(2));
        BigInteger privateKey;
        do {
            privateKey = new BigInteger(p.bitLength(), random);
        } while (privateKey.compareTo(BigInteger.ONE) < 0 || privateKey.compareTo(max) > 0);
        return privateKey;
    }

    public BigInteger getPublicKey() {
        return this.publicKey;
    }

    public BigInteger computeSharedSecret(BigInteger receivedPublicKey) {
        return receivedPublicKey.modPow(privateKey, p);
    }
}
