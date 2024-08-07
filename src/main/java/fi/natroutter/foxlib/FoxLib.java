package fi.natroutter.foxlib;

import fi.natroutter.foxlib.Handlers.DirectoryManager;
import fi.natroutter.foxlib.Handlers.FileManager;
import fi.natroutter.foxlib.Handlers.MultiFileManager;
import fi.natroutter.foxlib.data.FileResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class FoxLib {

    public String Version = "1.0.12";

    public static void print(Object message) {
        System.out.print(message.toString());
    }
    public static void printLn(Object message) {
        System.out.println(message.toString());
    }

    public static boolean isBetween(long number, long min, long max) {
        return number >= min && number <= max;
    }

}
