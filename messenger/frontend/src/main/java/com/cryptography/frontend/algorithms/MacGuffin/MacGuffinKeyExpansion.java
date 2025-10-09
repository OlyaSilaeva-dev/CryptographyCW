package com.cryptography.frontend.algorithms.MacGuffin;

import com.cryptography.frontend.algorithms.interfaces.KeyExpansion;

import java.util.Arrays;


public class MacGuffinKeyExpansion implements KeyExpansion {

    public static int[][] S = new int[][]{
            {2, 0, 0, 3, 3, 1, 1, 0, 0, 2, 3, 0, 3, 3, 2, 1, 1, 2, 2, 0, 0, 2, 2, 3, 1, 3, 3, 1, 0, 1, 1, 2,
                    0, 3, 1, 2, 2, 2, 2, 0, 3, 0, 0, 3, 0, 1, 3, 1, 3, 1, 2, 3, 3, 1, 1, 2, 1, 2, 2, 0, 1, 0, 0, 3},
            {3, 1, 1, 3, 2, 0, 2, 1, 0, 3, 3, 0, 1, 2, 0, 2, 3, 2, 1, 0, 0, 1, 3, 2, 2, 0, 0, 3, 1, 3, 2, 1,
                    0, 3, 2, 2, 1, 2, 3, 1, 2, 1, 0, 3, 3, 0, 1, 0, 1, 3, 2, 0, 2, 1, 0, 2, 3, 0, 1, 1, 0, 2, 3, 3},
            {2, 3, 0, 1, 3, 0, 2, 3, 0, 1, 1, 0, 3, 0, 1, 2, 1, 0, 3, 2, 2, 1, 1, 2, 3, 2, 0, 3, 0, 3, 2, 1,
                    3, 1, 0, 2, 0, 3, 3, 0, 2, 0, 3, 3, 1, 2, 0, 1, 3, 0, 1, 3, 0, 2, 2, 1, 1, 3, 2, 1, 2, 0, 1, 2},
            {1, 3, 3, 2, 2, 3, 1, 1, 0, 0, 0, 3, 3, 0, 2, 1, 1, 0, 0, 1, 2, 0, 1, 2, 3, 1, 2, 2, 0, 2, 3, 3,
                    2, 1, 0, 3, 3, 0, 0, 0, 2, 2, 3, 1, 1, 3, 3, 2, 3, 3, 1, 0, 1, 1, 2, 3, 1, 2, 0, 1, 2, 0, 0, 2},
            {0, 2, 2, 3, 0, 0, 1, 2, 1, 0, 2, 1, 3, 3, 0, 1, 2, 1, 1, 0, 1, 3, 3, 2, 3, 1, 0, 3, 2, 2, 3, 0,
                    0, 3, 0, 2, 1, 2, 3, 1, 2, 1, 3, 2, 1, 0, 2, 3, 3, 0, 3, 3, 2, 0, 1, 3, 0, 2, 1, 0, 0, 1, 2, 1},
            {2, 2, 1, 3, 2, 0, 3, 0, 3, 1, 0, 2, 0, 3, 2, 1, 0, 0, 3, 1, 1, 3, 0, 2, 2, 0, 1, 3, 1, 1, 3, 2,
                    3, 0, 2, 1, 3, 0, 1, 2, 0, 3, 2, 1, 2, 3, 1, 2, 1, 3, 0, 2, 0, 1, 2, 1, 1, 0, 3, 0, 3, 2, 0, 3},
            {0, 3, 3, 0, 0, 3, 2, 1, 3, 0, 0, 3, 2, 1, 3, 2, 1, 2, 2, 1, 3, 1, 1, 2, 1, 0, 2, 3, 0, 2, 1, 0,
                    1, 0, 0, 3, 3, 3, 3, 2, 2, 1, 1, 0, 1, 2, 2, 1, 2, 3, 3, 1, 0, 0, 2, 3, 0, 2, 1, 0, 3, 1, 0, 2},
            {3, 1, 0, 3, 2, 3, 0, 2, 0, 2, 3, 1, 3, 1, 1, 0, 2, 2, 3, 1, 1, 0, 2, 3, 1, 0, 0, 2, 2, 3, 1, 0,
                    1, 0, 3, 1, 0, 2, 1, 1, 3, 0, 2, 2, 2, 2, 0, 3, 0, 3, 0, 2, 2, 3, 3, 0, 3, 1, 1, 1, 1, 0, 2, 3}
    };

    /**
     * @param key Исходный ключ (массив байтов). Размер - 128 бит(16 байт)
     * @return массив раундовых ключей. Размер каждого - 48 бит(6 байт)
     */
    @Override
    public byte[][] keyExpansion(byte[] key) {
        if (key.length != 16) {
            key = to16Bytes(key);
        }
        byte[][] roundKeys = new byte[32][6];
        byte[] left = Arrays.copyOfRange(key, 0, 2);
        byte[] a = Arrays.copyOfRange(key, 2, 4);
        byte[] b = Arrays.copyOfRange(key, 4, 6);
        byte[] c = Arrays.copyOfRange(key, 6, 8);

        for (int i = 0; i < 32; i++) {
            byte[] t = new byte[2];

            for (int j = 0; j < 8; j++) {
                int inputA = (a[0] ^ key[8 + (j % 8)]) & 0x03;
                int inputB = (b[0] ^ key[8 + ((j + 1) % 8)]) & 0x03;
                int inputC = (a[0] ^ key[8 + ((j + 2) % 8)]) & 0x03;

                int index = (inputA << 4) | (inputB << 2) | inputC;
                int sboxValue = S[j][index] & 0x03;

                t[0] = (byte) ((t[0] << 2) | sboxValue);
            }

            for (int k = 0; k < left.length; k++) {
                left[k] ^= t[k % t.length];
            }

            roundKeys[i][0] = a[0];
            roundKeys[i][1] = b[0];
            roundKeys[i][2] = c[0];

            byte[] tmp = a;
            a = b;
            b = c;
            c = tmp;


            for (int k = 0; k < a.length; k++) {
                a[k] ^= t[k % t.length];
            }
        }

        return roundKeys;
    }

    public static byte[] to16Bytes(byte[] key) {
        byte[] key16 = new byte[16];
        int len = Math.min(key.length, 16);
        System.arraycopy(key, 0, key16, 0, len);
        if (len < 16) {
            for (int i = len; i < 16; i++) {
                key16[i] = 0;
            }
        }
        return key16;
    }

}
