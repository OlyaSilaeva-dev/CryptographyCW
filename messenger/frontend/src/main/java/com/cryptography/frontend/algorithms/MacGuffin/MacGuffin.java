package com.cryptography.frontend.algorithms.MacGuffin;

import com.cryptography.frontend.algorithms.interfaces.KeyExpansion;
import com.cryptography.frontend.algorithms.interfaces.SymmetricCipher;

import java.util.Arrays;

import static com.cryptography.frontend.algorithms.utils.Utils.xor;


public class MacGuffin implements SymmetricCipher {
    private static final int BLOCK_SIZE = 8;
    private byte[][] roundKeys;
    private static final KeyExpansion keyExpansion = new MacGuffinKeyExpansion();

    @Override
    public void setRoundKeys(byte[] key) {
        roundKeys = keyExpansion.keyExpansion(key);
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
        if (plaintext.length % BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("plaintext size must be 8 bytes, was " + plaintext.length);
        }
        byte[] left = Arrays.copyOfRange(plaintext, 0, 2);
        byte[] a = Arrays.copyOfRange(plaintext, 2, 4);
        byte[] b = Arrays.copyOfRange(plaintext, 4, 6);
        byte[] c = Arrays.copyOfRange(plaintext, 6, 8);

        for (int i = 0; i < 32; i++) {
            byte[] input = xor(xor(a, b), c);
            byte[] output = apply(input, roundKeys[i]);
            byte[] newLeft = xor(left, output);

            left = a;
            a = b;
            b = c;
            c = newLeft;
        }

        return collectResult(left, a, b, c);
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        if (ciphertext.length % BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("ciphertext size must be 8 bytes, was " + ciphertext.length);
        }
        byte[] left = Arrays.copyOfRange(ciphertext, 0, 2);
        byte[] a = Arrays.copyOfRange(ciphertext, 2, 4);
        byte[] b = Arrays.copyOfRange(ciphertext, 4, 6);
        byte[] c = Arrays.copyOfRange(ciphertext, 6, 8);

        for (int i = 31; i >= 0; i--) {
            byte[] input = xor(xor(left, a), b);
            byte[] output = apply(input, roundKeys[i]);
            byte[] newC = xor(c, output);

            c = b;
            b = a;
            a = left;
            left = newC;
        }

        return collectResult(left, a, b, c);
    }

    public static byte[] apply(byte[] input, byte[] roundKey) {
        byte[] output = new byte[2];
        output[0] = (byte) (input[0] ^ roundKey[0]);
        output[1] = (byte) (input[1] ^ roundKey[1]);
        output[0] ^= (byte) (input[1] ^ roundKey[2]);
        output[1] ^= (byte) (input[0] ^ roundKey[3]);
        return output;
    }

    private byte[] collectResult(byte[] left, byte[] a, byte[] b, byte[] c) {
        byte[] result = new byte[BLOCK_SIZE];
        System.arraycopy(left, 0, result, 0, 2);
        System.arraycopy(a, 0, result, 2, 2);
        System.arraycopy(b, 0, result, 4, 2);
        System.arraycopy(c, 0, result, 6, 2);
        return result;
    }

    @Override
    public int getBlockSize() {
        return BLOCK_SIZE;
    }
}
