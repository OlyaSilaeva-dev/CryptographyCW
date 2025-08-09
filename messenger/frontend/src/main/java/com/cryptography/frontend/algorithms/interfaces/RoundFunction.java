package com.cryptography.frontend.algorithms.interfaces;

public interface RoundFunction {
    /**
     * Выполняет раундовое преобразование входного блока с использованием раундового ключа.
     *
     * @param inputBlock Входной блок данных (массив байтов).
     * @param roundKey   Раундовый ключ (массив байтов).
     * @return Выходной блок данных (массив байтов).
     */
    byte[] roundConversion(byte[] inputBlock, byte[] roundKey);

    /**
     * Возвращает размер блока алгоритма в байтах.
     */
    int getBlockSize();

}

