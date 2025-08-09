//package com.cryptography.frontend.algorithms.DiffieHellman;
//
//import java.util.Random;
//
//public class DiffieHellman {
//    private static final Random rand = new Random();
//
//    // Проверка на простоту методом Миллера-Рабина
//    public static boolean isProbablePrime(long n, int k) {
//        if (n < 2) return false;
//        if (n == 2 || n == 3) return true;
//        if ((n & 1) == 0) return false;
//
//        long d = n - 1;
//        int r = 0;
//        while ((d & 1) == 0) {
//            d >>>= 1;
//            r++;
//        }
//
//        for (int i = 0; i < k; i++) {
//            long a = 2 + Math.abs(rand.nextLong()) % (n - 3);
//            long x = modPow(a, d, n);
//
//            if (x == 1 || x == n - 1) continue;
//
//            boolean passed = false;
//            for (int j = 0; j < r - 1; j++) {
//                x = modPow(x, 2, n);
//                if (x == n - 1) {
//                    passed = true;
//                    break;
//                }
//            }
//            if (!passed) return false;
//        }
//
//        return true;
//    }
//
//    // modPow: base^exp % mod
//    public static long modPow(long base, long exp, long mod) {
//        long result = 1;
//        base %= mod;
//
//        while (exp > 0) {
//            if ((exp & 1) == 1) {
//                result = modMult(result, base, mod);
//            }
//            base = modMult(base, base, mod);
//            exp >>>= 1;
//        }
//        return result;
//    }
//
//    // modMult: (a * b) % mod
//    public static long modMult(long a, long b, long mod) {
//        long result = 0;
//        a %= mod;
//        b %= mod;
//
//        while (b > 0) {
//            if ((b & 1) == 1) {
//                result = (result + a) % mod;
//            }
//            a = (a << 1) % mod;
//            b >>>= 1;
//        }
//        return result;
//    }
//
//    // Генерация случайного простого числа в диапазоне [low, high]
//    public static long generatePrime(long low, long high) {
//        while (true) {
//            long candidate = low + Math.abs(rand.nextLong()) % (high - low);
//            candidate |= 1; // делаем нечётным
//            if (isProbablePrime(candidate, 10)) {
//                return candidate;
//            }
//        }
//    }
//
//    // Поиск примитивного корня (неполный, но работает для безопасных простых)
//    public static long findPrimitiveRoot(long p) {
//        for (long g = 2; g < p; g++) {
//            boolean[] seen = new boolean[(int) p];
//            long val = 1;
//            for (int i = 0; i < p - 1; i++) {
//                val = modMult(val, g, p);
//                if (seen[(int) val]) {
//                    break;
//                }
//                seen[(int) val] = true;
//            }
//            if (val == 1) return g;
//        }
//        return -1;
//    }
//
////    public static void main(String[] args) {
////        // Шаг 1: общие параметры
////        long p = generatePrime(1000000, 2000000); // большое простое число
////        long g = findPrimitiveRoot(p);            // примитивный корень по модулю p
////
////        System.out.println("Public prime p: " + p);
////        System.out.println("Primitive root g: " + g);
////
////        // Шаг 2: генерация секретных ключей
////        long a = 2 + rand.nextInt(1000); // секрет Алисы
////        long b = 2 + rand.nextInt(1000); // секрет Боба
////
////        // Шаг 3: вычисляем открытые ключи
////        long A = modPow(g, a, p); // Алиса отправляет Бобу
////        long B = modPow(g, b, p); // Боб отправляет Алисе
////
////        // Шаг 4: каждая сторона вычисляет общий секрет
////        long secretA = modPow(B, a, p); // (g^b)^a % p
////        long secretB = modPow(A, b, p); // (g^a)^b % p
////
////        System.out.println("Alice's secret: " + secretA);
////        System.out.println("Bob's secret:   " + secretB);
////        System.out.println("Keys match:     " + (secretA == secretB));
////    }
//}