package fi.natroutter.foxlib.Handlers;

import fi.natroutter.foxlib.data.FileResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DirectoryManager {

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class Builder {
        private File directory = null;
        private String subDirectory = null;
        private Consumer<String> errorLogger = message -> {
            System.out.println("DirectoryManager/Error : " + message);
        };
        private Consumer<String> infoLogger = message -> {
            System.out.println("DirectoryManager/Info : " + message);
        };
        private Consumer<File> onInitialized = file -> {};

        public Builder setDirectory(File directory) {
            this.directory = directory;
            return this;
        }

        public Builder setSubDirectory(String subDirectory) {
            this.subDirectory = subDirectory;
            return this;
        }

        public Builder onInitialized(Consumer<File> response) {
            this.onInitialized = response;
            return this;
        }

        public Builder onErrorLog(Consumer<String> message) {
            this.errorLogger = message;
            return this;
        }

        public Builder onInfoLog(Consumer<String> message) {
            this.infoLogger = message;
            return this;
        }

        public DirectoryManager build() {
            return new DirectoryManager(this);
        }
    }

    private void info(Object message) {
        data.getInfoLogger().accept(message.toString());
    }
    private void error(Object message) {
        data.getErrorLogger().accept(message.toString());
    }

    private final Builder data;

    private File directory;

    private DirectoryManager(Builder builder) {
        this.data = builder;

        if (builder.getDirectory() != null) {
            directory = Path.of(builder.getDirectory().toString(), data.getSubDirectory()).toFile();
        } else {
            directory = Path.of(System.getProperty("user.dir"), data.getSubDirectory()).toFile();
        }

        data.onInitialized.accept(directory);
    }

    public void getFilesContent(Consumer<FileResponse> files) {

        File[] fileArray = directory.listFiles();
        if (fileArray == null) {
            error("Invalid Directory!");
            return;
        }
        if (fileArray.length > 0) {
            for (File file : fileArray) {
                new FileManager.Builder(file.getName())
                        .setDirectory(file.getParentFile())
                        .setExportResource(false)
                        .onErrorLog(data.errorLogger)
                        .onInfoLog(data.infoLogger)
                        .onInitialized(files)
                        .build();
            }
        } else {
            error("There are no files!");
        }

    }

}
