package fi.natroutter.foxlib;

import fi.natroutter.foxlib.files.FileResponse;
import lombok.Getter;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class FoxLib {

    public String Version = "1.2.3";

    public static void print(Object message) {
        System.out.print(message.toString());
    }
    public static void printLn(Object message) {
        System.out.println(message.toString());
    }

    public static boolean isBetween(long number, long min, long max) {
        return number >= min && number <= max;
    }


    public static boolean isBlank(String str) {
        if (str == null) return true;

        int len = str.length();
        if (len == 0) return true;

        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    private static String randomString(int length) {
        String table = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int)(table.length() * Math.random());
            sb.append(table.charAt(index));
        }
        return sb.toString();
    }
    public static String createCode(int partCount, int partLenght) {
        List<String> parts = new ArrayList<>();
        for(int s = 0; s < partCount; s++) {
            parts.add(randomString(partLenght));
        }
        return String.join("-", parts);
    }

    public static int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static FileResponse readFile(File file) {
        try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr)) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line).append(System.lineSeparator());
                line = br.readLine();
            }
            return new FileResponse(true, file.getName(), "OK", sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return new FileResponse(false, file.getName(), e.getMessage(), null);
        }
    }

    public static FileResponse writeFile(File file, String Content) {
        try(FileWriter fw = new FileWriter(file); BufferedWriter bw = new BufferedWriter(fw);) {
            if (!file.exists()) {
                file.createNewFile();
            }
            bw.write(Content);
            return new FileResponse(true, file.getName(), "OK", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new FileResponse(false, file.getName(), e.getMessage(), null);
        }
    }

    public static String getBasename(FileResponse resp) { return getBasename(resp.name()); }
    public static String getBasename(File file) { return getBasename(file.getName()); }
    public static String getBasename(String fileName) {
        Path path = Path.of(fileName);
        String fullName = path.getFileName().toString();
        int lastDotIndex = fullName.lastIndexOf('.');
        return (lastDotIndex > 0) ? fullName.substring(0, lastDotIndex) : fullName;
    }

    public static String getExt(FileResponse resp) { return getExt(resp.name()); }
    public static String getExt(File file) { return getExt(file.getName()); }
    public static String getExt(String fileName) {
        if (!fileName.contains(".")) return fileName;
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

}
