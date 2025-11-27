package fi.natroutter.foxlib.files;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;
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
        private List<String> allowedExtensions = new ArrayList<>();
        private FoxLogger logger = new FoxLogger.Builder()
                .setDebug(false)
                .setPruneOlderThanDays(35)
                .setSaveIntervalSeconds(300)
                .setLoggerName("DirectoryManager")
                .build();
        private Consumer<File> onInitialized = file -> {};

        public Builder(File directory) {
            this.directory = directory;
        }

        public Builder setAllowedExtensions(List<String> allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
            return this;
        }

        public Builder onInitialized(Consumer<File> response) {
            this.onInitialized = response;
            return this;
        }

        public DirectoryManager build() {
            return new DirectoryManager(this);
        }
    }

    private final Builder data;

    private File directory;

    private DirectoryManager(Builder builder) {
        this.data = builder;

        if (builder.getDirectory() == null) {
            data.logger.warn("Directory is not set properly using project root dir");
            directory = new File(System.getProperty("user.dir"));
        }

        if (!builder.getDirectory().isDirectory()) {
            data.logger.error("Selected directory path is not a valid directory!");
            return;
        }

        if (directory == null) {
            data.logger.error("Failed to load any directories!");
            return;
        }

        if (!directory.exists()) {
            directory.mkdirs();
        }

        data.onInitialized.accept(directory);
    }

    public void readAllFiles(Consumer<ReadResponse> file) {

        File[] fileArray = directory.listFiles();
        if (fileArray == null) {
            data.logger.error("Invalid Directory!");
            return;
        }

        if (fileArray.length > 0) {
            for (File entry : fileArray) {
                if (!entry.isFile()) continue;

                if (!data.getAllowedExtensions().isEmpty()) {
                    if (!data.getAllowedExtensions().contains(FoxLib.getExt(entry))) {
                        continue;
                    }
                }

                String logName = "@";
                File parentFile = directory.getParentFile();
                if (parentFile != null && parentFile.isDirectory()) {
                    logName = parentFile.getName() + "/@";
                }

                new FileManager.Builder(entry.getName())
                        .setDirectory(entry.getParentFile())
                        .setExportResource(false)
                        .setLogger(data.logger)
                        .setFileNameInLogs(logName)
                        .onInitialized(file)
                        .build();
            }
        } else {
            data.logger.error("There are no files!");
        }

    }

}
