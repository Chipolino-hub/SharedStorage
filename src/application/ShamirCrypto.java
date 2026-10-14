package application;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

public class ShamirCrypto {
    private static final SecureRandom random = new SecureRandom();
    private static final BigInteger PRIME = BigInteger.valueOf(257);

    public static List<String> splitSecret(String secret, int k, int n) {
        if (k > n || k < 2 || n > 10) throw new IllegalArgumentException("Неверные параметры");
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        List<List<BigInteger>> shares = new ArrayList<>(n);
        for (int i = 0; i < n; i++) shares.add(new ArrayList<>());
        for (byte b : bytes) {
            BigInteger[] coeffs = new BigInteger[k];
            coeffs[0] = BigInteger.valueOf(b & 0xFF);
            for (int j = 1; j < k; j++) {
                coeffs[j] = new BigInteger(8, random).mod(PRIME.subtract(BigInteger.ONE));
            }
            for (int i = 0; i < n; i++) {
                BigInteger x = BigInteger.valueOf(i + 1);
                BigInteger y = evaluate(coeffs, x);
                shares.get(i).add(y);
            }
        }
        List<String> hexShares = new ArrayList<>();
        for (List<BigInteger> share : shares) {
            StringBuilder sb = new StringBuilder();
            for (BigInteger val : share) {
                sb.append(String.format("%03x", val.intValue()));
            }
            hexShares.add(sb.toString());
        }
        return hexShares;
    }

    public static String recoverSecret(List<Integer> indices, List<String> sharesHex, int k) {
        if (indices.size() != k || sharesHex.size() != k) {
            throw new IllegalArgumentException("Количество индексов и долей должно быть равно K");
        }
        
        int byteCount = sharesHex.get(0).length() / 3;
        for (String hex : sharesHex) {
            if (hex.length() % 3 != 0) {
                throw new IllegalArgumentException("Некорректная длина hex-строки: " + hex.length());
            }
        }
        
        byte[] result = new byte[byteCount];
        for (int pos = 0; pos < byteCount; pos++) {
            List<BigInteger[]> points = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                String hex = sharesHex.get(i).substring(pos * 3, pos * 3 + 3);
                BigInteger x = BigInteger.valueOf(indices.get(i));
                BigInteger y = new BigInteger(hex, 16);
                points.add(new BigInteger[]{x, y});
            }
            BigInteger rec = lagrange(points, BigInteger.ZERO);
            result[pos] = (byte) (rec.intValue() & 0xFF);
        }
        return new String(result, StandardCharsets.UTF_8);
    }

    private static BigInteger evaluate(BigInteger[] coeffs, BigInteger x) {
        BigInteger res = BigInteger.ZERO;
        for (int i = coeffs.length - 1; i >= 0; i--) {
            res = res.multiply(x).add(coeffs[i]).mod(PRIME);
        }
        return res;
    }

    private static BigInteger lagrange(List<BigInteger[]> points, BigInteger x) {
        BigInteger res = BigInteger.ZERO;
        int k = points.size();
        for (int i = 0; i < k; i++) {
            BigInteger xi = points.get(i)[0];
            BigInteger yi = points.get(i)[1];
            BigInteger term = yi;
            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                BigInteger xj = points.get(j)[0];
                BigInteger num = x.subtract(xj).mod(PRIME);
                BigInteger den = xi.subtract(xj).mod(PRIME);
                term = term.multiply(num).multiply(den.modInverse(PRIME)).mod(PRIME);
            }
            res = res.add(term).mod(PRIME);
        }
        return res;
    }
}