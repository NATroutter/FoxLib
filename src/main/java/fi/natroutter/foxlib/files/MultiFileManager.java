package fi.natroutter.foxlib.files;

import fi.natroutter.foxlib.logger.FoxLogger;
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
        private FoxLogger logger = new FoxLogger.Builder()
                .setDebug(false)
                .setPruneOlderThanDays(35)
                .setSaveIntervalSeconds(300)
                .setLoggerName("MultiFileManager")
                .build();
        private Consumer<List<ReadResponse>> onInitialized = file -> {};

        public Builder(File directory) {
            this.directory = directory;
        }

        public Builder setLogger(FoxLogger logger) {
            this.logger = logger;
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

        public Builder onInitialized(Consumer<List<ReadResponse>> response) {
            this.onInitialized = response;
            return this;
        }

        public MultiFileManager build() {
            return new MultiFileManager(this);
        }
    }

    private MultiFileManager(Builder data) {

        List<ReadResponse> responses = new ArrayList<>();


        if (data.getDirectory() == null) {
            data.logger.warn("Directory is not set properly using project root dir");
            data.directory = new File(System.getProperty("user.dir"));
        }

        if (!data.getDirectory().isDirectory()) {
            data.logger.error("Selected directory path is not a valid directory!");
            return;
        }

        for(String name : data.getFileNames()) {
            File path = data.getDirectory();

            new FileManager.Builder(name)
                .setDirectory(path)
                .setExportResource(data.isExportResource())
                .setLoading(data.isLoading())
                .setLogger(data.logger)
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
