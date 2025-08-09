package com.cryptography.frontend.algorithms.rearrangingBits;

import com.cryptography.frontend.algorithms.enums.BitsOrder;

public class RearrangingBits {
    public static byte[] rearrangingBits(byte[] data, int[] pBox, BitsOrder bitsOrder, int indexFirstBit) {
        byte[] result = new byte[(pBox.length + 7) / 8];

        for (int i = 0; i < pBox.length; i++) {
            int bitIndex = (pBox[i] - indexFirstBit);
            int byteIndex = bitIndex / 8;
            int bitPos = bitIndex % 8;

            if (byteIndex < 0 || byteIndex >= data.length) {
                throw new IllegalArgumentException("pBox index out of range.");
            }

            boolean curBit;
            int curByte = data[byteIndex];
            if (bitsOrder == BitsOrder.LSB_FIRST) {
                curBit = ((curByte >>> bitPos) & 1) == 1;
            } else {
                curBit = ((curByte >>> (7 - bitPos)) & 1) == 1;
            }

            int destByteIndex = i / 8;
            int destBitPos = (bitsOrder == BitsOrder.LSB_FIRST) ? (i % 8) : 7 - (i % 8);

            if (curBit) {
                result[destByteIndex] |= (byte) (1 << destBitPos);
            }
        }
        return result;
    }
}
