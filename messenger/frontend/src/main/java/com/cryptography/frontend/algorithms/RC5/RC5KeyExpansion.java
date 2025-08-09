package com.cryptography.frontend.algorithms.RC5;

import com.cryptography.frontend.algorithms.interfaces.KeyExpansion;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RC5KeyExpansion implements KeyExpansion {
    private static final double e = Math.E;
    private static final double f = (1 + Math.sqrt(5)) / 2;

    private final int w;
    private final int r;
    private int b;
    private long P, Q;
    private final long mask;

    public RC5KeyExpansion(int w, int r) {
        if (w <= 0 || w > 64) {
            throw new IllegalArgumentException("w must be in range 1..64");
        }
        this.w = w;
        this.r = r;
        this.mask = (1L << w) - 1;
        generatingConstants();
        log.info("Generating RC5KeyExpansion P = {}, Q = {}",
                Long.toHexString(P),
                Long.toHexString(Q));
    }

    @Override
    public byte[][] keyExpansion(byte[] key) {
        this.b = key.length;
        int u = w / 8;
        int c = (b + u - 1) / u;
        int t = 2 * (r + 1);

        long[] L = convertingKeyToWords(key, u, c);
        long[] S = initializeS(t);

        mixingTheSecretKey(L, S, t, c);

        return convertSToBytes(S, u);
    }

    private void generatingConstants() {
        if (w > 0 && w < 64) {
            double pRaw = (e - 2) * (1L << w);
            double qRaw = (f - 1) * (1L << w);
            P = Math.round(pRaw) | 1;
            Q = Math.round(qRaw) | 1;
        } else if (w == 64) {
            double pFraction = (e - 2) * (1L << 63);
            P = (long) (pFraction * 2) | 1;

            double qFraction = (f - 1) * (1L << 63);
            Q = (long) (qFraction * 2) | 1;
        } else {
            throw new IllegalArgumentException("w must be in range 1..64");
        }
    }

    private long[] convertingKeyToWords(byte[] key, int u, int c) {
        long[] L = new long[c];
        for (int i = b - 1; i >= 0; i--) {
            L[i / u] = (L[i / u] << 8) | (key[i] & 0xFFL);
        }
        int i = 0;
        for (long l : L) {
            log.info("l[{}] = {}", i, Long.toHexString(l));
            i++;
        }
        return L;
    }

    private long[] initializeS(int t) {
        long[] S = new long[t];
        S[0] = P;
        for (int i = 1; i < t; i++) {
            S[i] = (S[i - 1] + Q) & mask;
        }
        int i = 0;
        for (long s : S) {
            log.info("s[{}] = {}", i, Long.toHexString(s));
            i++;
        }
        return S;
    }

    private void mixingTheSecretKey(long[] L, long[] S, int t, int c) {
        int iterations = 3 * Math.max(t, c);
        long A = 0, B = 0;
        int i = 0, j = 0;

        for (int k = 0; k < iterations; k++) {
            A = S[i] = rotateLeft((S[i] + A + B) & mask, 3);
            B = L[j] = rotateLeft((L[j] + A + B) & mask, (int)((A + B) % w));
            i = (i + 1) % t;
            j = (j + 1) % c;
        }
    }

    private byte[][] convertSToBytes(long[] S, int u) {
        byte[][] result = new byte[S.length][u];
        for (int i = 0; i < S.length; i++) {
            for (int j = 0; j < u; j++) {
                result[i][j] = (byte)((S[i] >>> (8 * j)) & 0xFF);
            }
        }
        return result;
    }

    private long bytesToLong(byte[] wordBytes) {
        long value = 0;
        for (int i = 0; i < Math.min(wordBytes.length, 8); i++) {
            value |= (wordBytes[i] & 0xffL) << (i * 8);
        }
        return value;
    }


    private long rotateLeft(long value, int shift) {
        shift %= w;
        return ((value << shift) | (value >>> (w - shift))) & mask;
    }
}
