package fi.natroutter.foxlib;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FoxLib {

    public String Version = "1.1.2";

    public static void print(Object message) {
        System.out.print(message.toString());
    }
    public static void printLn(Object message) {
        System.out.println(message.toString());
    }

    public static boolean isBetween(long number, long min, long max) {
        return number >= min && number <= max;
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

}
