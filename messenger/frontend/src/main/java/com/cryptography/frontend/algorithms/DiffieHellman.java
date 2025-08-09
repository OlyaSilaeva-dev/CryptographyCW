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
        this.privateKey = new BigInteger(65537, new SecureRandom());
        this.publicKey = g.modPow(privateKey, p);
    }

    public BigInteger getPublicKey() {
        return this.publicKey;
    }

    public BigInteger computeSharedSecret(BigInteger receivedPublicKey) {
        return receivedPublicKey.modPow(publicKey, p);
    }

    public static BigInteger generateSafePrime() {
        SecureRandom rand = new SecureRandom();
        return BigInteger.probablePrime(512, rand);
    }
}
