package fi.natroutter.foxlib.Handlers;

public interface FoxUtilities {

    default void print(String message) {
        System.out.print(message + "\u001B[0m");
    }

    default void printLine(String message) {
        System.out.println(message + "\u001B[0m");
    }

}
