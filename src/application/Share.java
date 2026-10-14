package application;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.regex.Pattern;

public class Share {
    private int index, k, n;
    private String data, originalFileName, hash;
    private String dataHash;
    private static final Pattern HEX_PAT = Pattern.compile("^[0-9a-fA-F]+$");

    public Share(int index, int k, int n, String data, String originalFileName, String hash) {
        this.index = index;
        this.k = k;
        this.n = n;
        this.data = data;
        this.originalFileName = originalFileName;
        this.hash = hash;
        this.dataHash = calculateDataHash(data);
    }

    public void saveToFile(String dir) throws Exception {
        String content = "=== ДОЛЯ ===\n" +
                "Версия: 2.1\n" +
                "Файл: " + originalFileName + "\n" +
                "K=" + k + ", N=" + n + "\n" +
                "Доля: " + index + "\n" +
                "Хеш исходного файла: " + hash + "\n" +
                "Хеш данных доли: " + dataHash + "\n" +
                "Данные (hex): " + data + "\n" +
                "Для восстановления нужно " + k + " долей.\n" +
                "ВНИМАНИЕ: Не изменяйте содержимое файла, иначе доля станет недействительной!";
        String fileName = originalFileName.replace(".txt", "") + "_доля_" + index + ".txt";
        Files.writeString(Paths.get(dir, fileName), content, StandardCharsets.UTF_8);
    }

    public static Share loadFromFile(File file) throws Exception {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        int index = 0, k = 0, n = 0;
        String name = "", hash = "", dataStr = "", storedDataHash = "";
        String[] lines = content.split("\n");
        String shareVersion = null;
        
        for (String line : lines) {
            if (line.startsWith("Версия: ")) {
                shareVersion = line.substring("Версия: ".length()).trim();
            }
            if (line.contains("Файл: ")) {
                name = line.substring(line.indexOf(": ") + 2);
            }
            if (line.contains("K=")) {
                String[] parts = line.split(", ");
                k = Integer.parseInt(parts[0].split("=")[1]);
                n = Integer.parseInt(parts[1].split("=")[1]);
            }
            if (line.contains("Доля: ")) {
                index = Integer.parseInt(line.substring(line.indexOf(": ") + 2));
            }
            if (line.contains("Хеш исходного файла: ")) {
                hash = line.substring(line.indexOf(": ") + 2);
            }
            if (line.contains("Хеш данных доли: ")) {
                storedDataHash = line.substring(line.indexOf(": ") + 2);
            }
            if (line.contains("Данные (hex): ")) {
                dataStr = line.substring(line.indexOf(": ") + 2);
            }
        }
        
        dataStr = dataStr.trim().replaceAll("\\s+", "");
        
        if (!HEX_PAT.matcher(dataStr).matches()) {
            throw new Exception("Неверный формат данных доли");
        }
        
        if (!"1.0".equals(shareVersion) && !"2.1".equals(shareVersion)) {
            throw new Exception("Неподдерживаемая версия доли");
        }
        {
            String calculatedDataHash = calculateDataHash(dataStr);
            if (!calculatedDataHash.equals(storedDataHash)) {
                throw new Exception("Целостность доли нарушена! Файл был изменен.");
            }
        }
        
        Share share = new Share(index, k, n, dataStr, name, hash);
        return share;
    }

    private static String calculateDataHash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "error";
        }
    }

    public int getIndex() { return index; }
    public int getK() { return k; }
    public int getN() { return n; }
    public String getData() { return data; }
    public String getOriginalFileName() { return originalFileName; }
    public String getHash() { return hash; }
    public String getDataHash() { return dataHash; }
}