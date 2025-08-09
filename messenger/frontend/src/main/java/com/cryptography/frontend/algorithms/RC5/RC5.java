package com.cryptography.frontend.algorithms.RC5;

import com.cryptography.frontend.algorithms.interfaces.KeyExpansion;
import com.cryptography.frontend.algorithms.interfaces.SymmetricCipher;

public class RC5 implements SymmetricCipher {
    private final KeyExpansion keyExpansion;
    private long[] roundKeys;
    private final int w;
    private final int r;
    private final int b;
    private final int blockSize;
    private final long mask;

    /**
     * RC5
     * @param w - размер слова в битах (32 или 64)
     * @param r - количество раундов (0..255)
     * @param b - размер ключа в байтах (0..255)
     */
    public RC5(int w, int r, int b) {
        this.w = w;
        this.r = r;
        this.b = b;
        mask = (1L << w) - 1;
        this.blockSize = 2 * w / 8;
        this.keyExpansion = new RC5KeyExpansion(w, r);
    }

    @Override
    public void setRoundKeys(byte[] key) {
        byte[][] rawKeys = keyExpansion.keyExpansion(key); // [2*(r+1)][w/8]
        roundKeys = new long[rawKeys.length];
        for (int i = 0; i < rawKeys.length; i++) {
            roundKeys[i] = bytesToWord(rawKeys[i], 0);
        }
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
        if (plaintext.length != blockSize)
            throw new IllegalArgumentException("Invalid plaintext size");

        long A = bytesToWord(plaintext, 0);
        long B = bytesToWord(plaintext, w / 8);

        A = (A + roundKeys[0]) & mask;
        B = (B + roundKeys[1]) & mask;

        for (int i = 1; i <= r; i++) {
            A = (rotateLeft(A ^ B, B) + roundKeys[2 * i]) & mask;
            B = (rotateLeft(B ^ A, A) + roundKeys[2 * i + 1]) & mask;
        }

        byte[] encryptedBlock = new byte[2 * w / 8];
        wordToBytes(A, encryptedBlock, 0);
        wordToBytes(B, encryptedBlock, w / 8);
        return encryptedBlock;
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        if (ciphertext.length != getBlockSize()) {
            throw new IllegalArgumentException("Invalid ciphertext size");
        }

        long A = bytesToWord(ciphertext, 0);
        long B = bytesToWord(ciphertext, w / 8);

        for (int i = r; i >= 1; i--) {
            B = rotateRight((B - roundKeys[2 * i + 1]) & mask, A) ^ A;
            A = rotateRight((A - roundKeys[2 * i]) & mask, B) ^ B;
        }

        B = (B - roundKeys[1]) & mask;
        A = (A - roundKeys[0]) & mask;

        byte[] decryptedBlock = new byte[getBlockSize()];
        wordToBytes(A, decryptedBlock, 0);
        wordToBytes(B, decryptedBlock, w / 8);

        return decryptedBlock;
    }

    @Override
    public int getBlockSize() {
        return blockSize;
    }

    private long bytesToWord(byte[] bytes, int offset) {
        long word = 0;
        for (int i = 0; i < w / 8; i++) {
            word |= (bytes[offset + i] & 0xffL) << (i * 8);
        }
        return word;
    }

    private long rotateLeft(long value, long shift) {
        shift %= w;
        return ((value << shift) | (value >>> (w - shift))) & mask;
    }

    private long rotateRight(long value, long shift) {
        shift %= w;
        return ((value >>> shift) | (value << (w - shift))) & mask;
    }

    private void wordToBytes(long word, byte[] bytes, int offset) {
        for (int i = 0; i < w / 8; i++) {
            bytes[offset + i] = (byte) (word >>> (i * 8));
        }
    }
}
