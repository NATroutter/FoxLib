package fi.natroutter.foxlib.Handlers;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.data.FileResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MultiFileManager {

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class Builder {
        private List<String> fileNames;
        private boolean exportResource = true;
        private boolean loading = true;
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
        public Builder setExportResource(boolean value) {
            this.exportResource = value;
            return this;
        }
        public Builder setLoading(boolean value) {
            this.loading = value;
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

    private MultiFileManager(Builder data) {
        this.data = data;

        List<FileResponse> responses = new ArrayList<>();


        for(String name : data.getFileNames()) {
            File path;
            if (data.getDirectory() != null) {
                path = Path.of(data.getDirectory().toString()).toFile();
            } else {
                path = Path.of(System.getProperty("user.dir")).toFile();
            }
            new FileManager.Builder(name)
                .setDirectory(path)
                .setExportResource(data.isExportResource())
                .setLoading(data.isLoading())
                .onInfoLog(data.infoLogger)
                .onErrorLog(data.errorLogger)
                .onInitialized(file -> {
                    if (file != null && file.success()) {
                        responses.add(file);
                    }
                })
                .build();
        }
        data.onInitialized.accept(responses);
    }


}
