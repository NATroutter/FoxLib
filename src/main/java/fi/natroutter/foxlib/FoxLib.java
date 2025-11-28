package fi.natroutter.foxlib;

import fi.natroutter.foxlib.config.ConfigProvider;
import fi.natroutter.foxlib.files.ReadResponse;
import fi.natroutter.foxlib.files.WriteResponse;
import lombok.Getter;

import java.io.*;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class FoxLib {

    public String Version = "1.3.6";

    public static void print(Object message) {
        System.out.print(message.toString());
    }
    public static void println(Object message) {
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

    public static <T> List<T> addToList(ArrayList<T> list, T item) {
        if (!list.contains(item)) {
            list.add(item);
        }
        return list;
    }

    private static String randomString(int length) {
        return randomString(length,0L);
    }
    private static String randomString(int length, long seed) {
        String table = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        if (seed > 0L) {
            random.setSeed(seed);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(table.length());
            sb.append(table.charAt(index));
        }
        return sb.toString();
    }
    public static String createSerialCode(int partCount, int partLenght) {
        List<String> parts = new ArrayList<>();
        for(int s = 0; s < partCount; s++) {
            parts.add(randomString(partLenght));
        }
        return String.join("-", parts);
    }

    public static int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static void openURL(String url) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
        } else if (os.contains("mac")) {
            Runtime.getRuntime().exec("open " + url);
        } else if (os.contains("nix") || os.contains("nux")) {
            Runtime.getRuntime().exec("xdg-open " + url);
        }
    }

}
