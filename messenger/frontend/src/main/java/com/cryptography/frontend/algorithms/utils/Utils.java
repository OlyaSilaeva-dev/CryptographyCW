package com.cryptography.frontend.algorithms.utils;

public class Utils {
    public static byte[] xor(byte[] a, byte[] b) {
        int maxLength = Math.max(a.length, b.length);
        byte[] result = new byte[maxLength];

        for (int i = 0; i < maxLength; i++) {
            byte aByte = (i < a.length) ? a[i] : 0;
            byte bByte = (i < b.length) ? b[i] : 0;
            result[i] = (byte) (aByte ^ bByte);
        }

        return result;
    }
}
