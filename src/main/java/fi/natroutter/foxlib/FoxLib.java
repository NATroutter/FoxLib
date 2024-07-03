package fi.natroutter.foxlib;

import fi.natroutter.foxlib.Handlers.DirectoryManager;
import fi.natroutter.foxlib.data.FileResponse;
import lombok.Getter;

@Getter
public class FoxLib {

    public String Version = "1.0.10";


    public static void print(Object message) {
        System.out.print(message.toString());
    }
    public static void printLn(Object message) {
        System.out.println(message.toString());
    }

}
