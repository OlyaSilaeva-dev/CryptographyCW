package com.cryptography.frontend.algorithms.interfaces;

public interface KeyExpansion {
    /**
     * Генерирует раундовые ключи на основе входного ключа.
     *
     * @param key Исходный ключ (массив байтов).
     * @return Двумерный массив, где каждый элемент - раундовый ключ.
     */
    byte[][] keyExpansion(byte[] key);
}
