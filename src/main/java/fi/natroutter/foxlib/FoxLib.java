package fi.natroutter.foxlib;

import fi.natroutter.foxlib.Handlers.DirectoryManager;
import fi.natroutter.foxlib.data.FileResponse;
import lombok.Getter;

@Getter
public class FoxLib {

    public String Version = "1.0.9";


    public static void print(Object message) {
        System.out.print(message.toString());
    }
    public static void printLine(Object message) {
        System.out.println(message.toString());
    }

    public static void main(String[] args) {

        DirectoryManager dm = new DirectoryManager.Builder()
                .setSubDirectory("embeds")
                .onInitialized(file -> printLine("Path: " +file.toString()))
                .build();

        dm.getFilesContent(file -> {
            printLine(file.message() + " - " + file.fileContent());
        });



    }

}
