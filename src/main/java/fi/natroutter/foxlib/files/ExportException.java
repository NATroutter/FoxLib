package fi.natroutter.foxlib.files;

import lombok.Getter;

import java.io.File;

@Getter
public class ExportException extends Exception {

    private final File file;
    private final String resourceFile;

    public ExportException(String message, File file, String resourceFile) {
        super(message + " : " + resourceFile + " -> " + file);
        this.file = file;
        this.resourceFile = resourceFile;
    }
    public ExportException(String message, Throwable cause, File file, String resourceFile) {
        super(message + " : " + resourceFile + " -> " + file, cause);
        this.file = file;
        this.resourceFile = resourceFile;
    }

}
