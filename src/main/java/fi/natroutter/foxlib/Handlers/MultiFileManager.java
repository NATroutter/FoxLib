package fi.natroutter.foxlib.Handlers;

import fi.natroutter.foxlib.data.FileResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MultiFileManager {

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class Builder {
        private List<String> fileNames;
        private boolean exportFiles = true;
        private File directory = null;
        private Consumer<String> errorLogger = message -> {
            System.out.println("MultiFileManager/Error : " + message);
        };
        private Consumer<String> infoLogger = message -> {
            System.out.println("MultiFileManager/Info : " + message);
        };
        private Consumer<List<FileResponse>> onInitialized = file -> {};

        public Builder setDirectory(File directory) {
            this.directory = directory;
            return this;
        }
        public Builder registerFiles(List<String> fileNames) {
            this.fileNames = fileNames;
            return this;
        }
        public Builder setExportFiles(boolean value) {
            this.exportFiles = value;
            return this;
        }

        public Builder onInitialized(Consumer<List<FileResponse>> response) {
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

        public MultiFileManager build() {
            return new MultiFileManager(this);
        }
    }

    private void info(Object message) {
        data.getInfoLogger().accept(message.toString());
    }
    private void error(Object message) {
        data.getErrorLogger().accept(message.toString());
    }

    private final Builder data;

    private MultiFileManager(Builder builder) {
        this.data = builder;

        List<FileResponse> responses = new ArrayList<>();

        for(String name : data.getFileNames()) {

            new FileManager.Builder(name)
                .setDirectory(data.getDirectory())
                .setExportResource(data.isExportFiles())
                .onInfoLog(data.infoLogger)
                .onErrorLog(data.errorLogger)
                .onInitialized(file -> {
                    if (file.success()) {
                        responses.add(file);
                    }
                })
                .build();
        }
        data.onInitialized.accept(responses);
    }


}
