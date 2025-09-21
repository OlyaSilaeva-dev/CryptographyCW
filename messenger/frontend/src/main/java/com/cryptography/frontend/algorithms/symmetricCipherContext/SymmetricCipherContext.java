package com.cryptography.frontend.algorithms.symmetricCipherContext;

import com.cryptography.frontend.algorithms.enums.EncryptionMode;
import com.cryptography.frontend.algorithms.enums.PaddingMode;
import com.cryptography.frontend.algorithms.interfaces.SymmetricCipher;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class SymmetricCipherContext {
    @Getter
    private final SymmetricCipher cipher;
    private final byte[] key;
    private final EncryptionMode encryptionMode;
    private final PaddingMode paddingMode;
    private byte[] iv;
    private final byte delta;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public SymmetricCipherContext(SymmetricCipher symmetricCipher, byte[] key, EncryptionMode encryptionMode,
                                  PaddingMode paddingMode, byte[] iv, byte... params) {
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        this.key = key;
        this.cipher = symmetricCipher;
        this.encryptionMode = encryptionMode;
        this.paddingMode = paddingMode;
        this.iv = iv;
        if (encryptionMode == EncryptionMode.RandomDelta && params.length > 0) {
            this.delta = params[0];
        } else {
            this.delta = generateRandomDelta();
        }
        this.cipher.setRoundKeys(key);
    }

    private byte generateRandomDelta() {
        SecureRandom random = new SecureRandom();
        return (byte) random.nextInt(256); // От -128 до 127
    }

    public CompletableFuture<byte[]> encryptAsync(byte[] plaintext) {
        return CompletableFuture.supplyAsync(() -> encrypt(plaintext), executor);
    }

    public byte[] encrypt(byte[] plaintext) {
        int blockSize = this.cipher.getBlockSize();

        plaintext = applyPadding(plaintext, blockSize);
        log.info("Plaintext with padding: {}", Arrays.toString(plaintext));

        int blockCnt = (int) Math.ceil(plaintext.length / (double) blockSize);
        byte[][] blocks = new byte[blockCnt][blockSize];

        for (int i = 0; i < blockCnt; i++) {
            System.arraycopy(plaintext, i * blockSize, blocks[i], 0, blockSize);
        }

        log.info(Arrays.toString(blocks));
        return switch (encryptionMode) {
            case ECB -> ECBEncrypt(blocks, blockSize, blockCnt);
            case CBC -> CBCEncrypt(blocks, blockSize, blockCnt);
            case PCBC -> PCBCEncrypt(blocks, blockSize, blockCnt);
            case CFB -> CFBEncrypt(blocks, blockSize, blockCnt);
            case OFB -> OFBEncrypt(blocks, blockSize, blockCnt);
            case CTR -> CTREncrypt(blocks, blockSize, blockCnt);
            case RandomDelta -> randomDeltaEncrypt(blocks, blockSize, blockCnt);
        };
    }

    private byte[] applyPadding(byte[] data, int blockSize) {
        int paddingSize = (blockSize - data.length % blockSize) % blockSize;
        byte[] padded = Arrays.copyOf(data, data.length + paddingSize);

        switch (paddingMode) {
            case Zeros -> Arrays.fill(padded, data.length, padded.length, (byte) 0);
            case ANSI_X923 -> {
                Arrays.fill(padded, data.length, padded.length - 1, (byte) 0);
                padded[padded.length - 1] = (byte) paddingSize;
            }
            case PKCS7 -> Arrays.fill(padded, data.length, padded.length, (byte) paddingSize);
            case ISO_10126 -> {
                SecureRandom random = new SecureRandom();
                byte[] randomBytes = new byte[paddingSize - 1];
                random.nextBytes(randomBytes);
                System.arraycopy(randomBytes, 0, padded, data.length, randomBytes.length);
                padded[padded.length - 1] = (byte) paddingSize;
            }
        }
        return padded;
    }

    private byte[] XOR2Blocks(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }

    private byte[] ECBEncrypt(byte[][] blocks, int blockSize, int blockCnt) {
        byte[] ciphertext = new byte[blockCnt * blockSize];

        Arrays.parallelSetAll(blocks, i -> cipher.encrypt(blocks[i]));

        for (int i = 0; i < blockCnt; i++) {
            System.arraycopy(blocks[i], 0, ciphertext, i * blockSize, blockSize);
        }
        return ciphertext;
    }

    private byte[] CBCEncrypt(byte[][] blocks, int blockSize, int blockCnt) {
        byte[] prevCipherBlock = iv.clone();
        byte[] ciphertext = new byte[blockCnt * blockSize];

        for (int i = 0; i < blockCnt; i++) {
            byte[] xorBlock = XOR2Blocks(blocks[i], prevCipherBlock);
            byte[] cipherBlock = cipher.encrypt(xorBlock);
            System.arraycopy(cipherBlock, 0, ciphertext, i * blockSize, blockSize);
            prevCipherBlock = cipherBlock.clone();
        }
        return ciphertext;
    }

    private byte[] PCBCEncrypt(byte[][] blocks, int blockSize, int blockCnt) {
        byte[] prevCipherBlock = iv.clone();
        byte[] ciphertext = new byte[blockCnt * blockSize];

        for (int i = 0; i < blockCnt; i++) {
            byte[] xorBlock = XOR2Blocks(blocks[i], prevCipherBlock);
            byte[] cipherBlock = cipher.encrypt(xorBlock);
            System.arraycopy(cipherBlock, 0, ciphertext, i * blockSize, blockSize);
            prevCipherBlock = XOR2Blocks(blocks[i], cipherBlock);
        }

        return ciphertext;
    }

    private byte[] CFBEncrypt(byte[][] blocks, int blockSize, int blockCnt) {
        byte[] ciphertext = new byte[blockCnt * blockSize];
        byte[] feedback = iv.clone();

        for (int i = 0; i < blockCnt; i++) {
            byte[] cipherBlock = cipher.encrypt(feedback);
            byte[] xorResult = XOR2Blocks(blocks[i], cipherBlock);
            System.arraycopy(xorResult, 0, ciphertext, i * blockSize, blockSize);
            feedback = xorResult.clone();
        }
        return ciphertext;
    }

    private byte[] OFBEncrypt(byte[][] blocks, int blockSize, int blockCnt) {
        byte[] ciphertext = new byte[blockCnt * blockSize];
        byte[] feedback = iv.clone();

        for (int i = 0; i < blockCnt; i++) {
            feedback = cipher.encrypt(feedback);
            byte[] xorResult = XOR2Blocks(blocks[i], feedback);
            System.arraycopy(xorResult, 0, ciphertext, i * blockSize, blockSize);
        }
        return ciphertext;
    }

    private byte[] CTREncrypt(byte[][] blocks, int blockSize, int blockCnt) {
        byte[] ciphertext = new byte[blockCnt * blockSize];

        Arrays.parallelSetAll(blocks, i -> {
            byte[] localCounter = addCounter(iv, i);
            byte[] encryptedCounter = cipher.encrypt(localCounter);
            byte[] xorResult = XOR2Blocks(blocks[i], encryptedCounter);
            System.arraycopy(xorResult, 0, ciphertext, i * blockSize, blockSize);
            return blocks[i];
        });

        return ciphertext;
    }

    private byte[] addCounter(byte[] iv, int increment) {
        byte[] counter = iv.clone();
        int carry = increment;
        for (int j = counter.length - 1; j >= 0 && carry > 0; j--) {
            int sum = (counter[j] & 0xFF) + (carry & 0xFF);
            counter[j] = (byte) sum;
            carry = sum >>> 8;
        }
        return counter;
    }


    private byte[] randomDeltaEncrypt(byte[][] blocks, int blockSize, int blockCnt) {
        byte[] ciphertext = new byte[blockCnt * blockSize];
        byte[] counter = iv.clone();

        for (int i = 0; i < blockCnt; i++) {
            byte[] xorBlock = XOR2Blocks(blocks[i], counter);
            byte[] cipherBlock = cipher.encrypt(xorBlock);
            System.arraycopy(cipherBlock, 0, ciphertext, i * blockSize, blockSize);
            increaseCounterByDelta(counter);
        }
        return ciphertext;
    }

    private void increaseCounterByDelta(byte[] counter) {
        int carry = delta & 0xFF;
        for (int i = counter.length - 1; i >= 0; i--) {
            int sum = (counter[i] & 0xFF) + carry;
            counter[i] = (byte) sum;
            carry = sum >> 8;
            if (carry == 0) break;
        }
    }

    public CompletableFuture<byte[]> decryptAsync(byte[] plaintext) {
        return CompletableFuture.supplyAsync(() -> decrypt(plaintext), executor);
    }

    public byte[] decrypt(byte[] ciphertext) {
        int blockSize = this.cipher.getBlockSize();
        int blockCnt = (int) Math.ceil((double) ciphertext.length / (double) blockSize);
        byte[][] cipherBlocks = new byte[blockCnt][blockSize];

        for (int i = 0; i < blockCnt; i++) {
            System.arraycopy(ciphertext, i * blockSize, cipherBlocks[i], 0, blockSize);
        }

        log.info(Arrays.deepToString(cipherBlocks));
        byte[] result = switch (encryptionMode) {
            case ECB -> ECBDecrypt(cipherBlocks, blockSize, blockCnt);
            case CBC -> CBCDecrypt(cipherBlocks, blockSize, blockCnt);
            case PCBC -> PCBCDecrypt(cipherBlocks, blockSize, blockCnt);
            case CFB -> CFBDecrypt(cipherBlocks, blockSize, blockCnt);
            case OFB -> OFBDecrypt(cipherBlocks, blockSize, blockCnt);
            case CTR -> CTRDecrypt(cipherBlocks, blockSize, blockCnt);
            case RandomDelta -> RandomDeltaDecrypt(cipherBlocks, blockSize, blockCnt);
        };

        return removePadding(result);
    }

    private byte[] removePadding(byte[] data) {
        int padLength;
        switch (paddingMode) {
            case PKCS7, ANSI_X923, ISO_10126 -> {
                padLength = data[data.length - 1] & 0xFF;
                if (!(padLength == 0) && !(padLength > cipher.getBlockSize())) {
                    return Arrays.copyOfRange(data, 0, data.length - padLength);
                }
                return data;
            }
            case Zeros -> {
                int i = data.length - 1;
                while (i >= 0 && data[i] == 0) {
                    i--;
                }
                return Arrays.copyOf(data, i + 1);
            }
            default -> throw new IllegalArgumentException("Unsupported padding mode: " + paddingMode);
        }
    }

    private byte[] ECBDecrypt(byte[][] blocks, int blockSize, int blockCnt) {
        byte[] plaintext = new byte[blockCnt * blockSize];
        for (int i = 0; i < blockCnt; i++) {
            byte[] plainBlock = cipher.decrypt(blocks[i]);
            System.arraycopy(plainBlock, 0, plaintext, i * blockSize, blockSize);
        }
        log.info("plaintext after decrypt: " + Arrays.toString(plaintext));
        return plaintext;
    }

    private byte[] CBCDecrypt(byte[][] blocks, int blockSize, int blockCnt) {
        byte[] plaintext = new byte[blockCnt * blockSize];
        byte[] prevBlock = iv.clone();

        for (int i = 0; i < blockCnt; i++) {
            byte[] decryptedBlock = cipher.decrypt(blocks[i]);
            byte[] plainBlock = XOR2Blocks(decryptedBlock, prevBlock);
            System.arraycopy(plainBlock, 0, plaintext, i * blockSize, blockSize);
            prevBlock = blocks[i];
        }
        return plaintext;
    }

    private byte[] PCBCDecrypt(byte[][] blocks, int blockSize, int blockCnt) {
        byte[] plaintext = new byte[blockCnt * blockSize];
        byte[] prevPlain = iv.clone();

        for (int i = 0; i < blockCnt; i++) {
            byte[] decryptedBlock = cipher.decrypt(blocks[i]);
            byte[] plainBlock = XOR2Blocks(decryptedBlock, prevPlain);
            System.arraycopy(plainBlock, 0, plaintext, i * blockSize, blockSize);

            if (i < blockCnt - 1) {
                prevPlain = XOR2Blocks(plainBlock, blocks[i]);
            }
        }
        return plaintext;
    }

    private byte[] CFBDecrypt(byte[][] blocks, int blockSize, int blockCnt) {
        byte[] plaintext = new byte[blockCnt * blockSize];
        byte[] prevCipher = iv.clone();

        for (int i = 0; i < blockCnt; i++) {
            byte[] cipherStream = cipher.encrypt(prevCipher);
            byte[] plainBlock = XOR2Blocks(cipherStream, blocks[i]);
            System.arraycopy(plainBlock, 0, plaintext, i * blockSize, blockSize);
            prevCipher = blocks[i];
        }
        return plaintext;
    }

    private byte[] OFBDecrypt(byte[][] blocks, int blockSize, int blockCnt) {
        return OFBEncrypt(blocks, blockSize, blockCnt);
    }

    private byte[] CTRDecrypt(byte[][] blocks, int blockSize, int blockCnt) {
        return CTREncrypt(blocks, blockSize, blockCnt);
    }

    private byte[] RandomDeltaDecrypt(byte[][] blocks, int blockSize, int blockCnt) {
        byte[] ciphertext = new byte[blockCnt * blockSize];
        byte[] counter = iv.clone();

        for (int i = 0; i < blockCnt; i++) {
            byte[] plainBlock = cipher.decrypt(blocks[i]);
            byte[] xorBlock = XOR2Blocks(plainBlock, counter);
            System.arraycopy(xorBlock, 0, ciphertext, i * blockSize, blockSize);
            increaseCounterByDelta(counter);
        }
        return ciphertext;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
