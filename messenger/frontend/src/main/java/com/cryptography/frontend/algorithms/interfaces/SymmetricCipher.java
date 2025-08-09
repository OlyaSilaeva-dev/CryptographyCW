package com.cryptography.frontend.algorithms.interfaces;

public interface SymmetricCipher {
    /**
     * Устанавливает раундовые ключи для шифрования и дешифрования.
     *
     * @param key Основной ключ (массив байтов), который будет использоваться для генерации раундовых ключей.
     */
    void setRoundKeys(byte[] key);

    /**
     * Шифрует блок данных, используя ранее установленный ключ.
     *
     * @param plaintext Открытый текст (массив байтов).
     * @return Зашифрованный блок (массив байтов).
     */
    byte[] encrypt(byte[] plaintext);

    /**
     * Дешифрует блок данных, используя ранее установленный ключ.
     *
     * @param ciphertext Зашифрованные данные (массив байтов).
     * @return Расшифрованный блок (массив байтов).
     */
    byte[] decrypt(byte[] ciphertext);

    /**
     * Возвращает размер блока алгоритма в байтах.
     */
    int getBlockSize();
 }
