package fi.natroutter.foxlib;

import lombok.Getter;

public class FoxLib {

    @Getter
    public String Version = "1.0.1";

    public static void print(String message) {
        System.out.print(message + "\u001B[0m");
    }

    public static void printLine(String message) {
        System.out.println(message + "\u001B[0m");
    }

}
