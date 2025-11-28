package fi.natroutter.foxlib.files;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DirectoryManager {

    @Getter @AllArgsConstructor
    public static class Builder {
        private File directory;
        private List<String> allowedExtensions = new ArrayList<>();
        private List<String> excludeFromReading = new ArrayList<>();
        private List<String> exportingFiles = new ArrayList<>();
        private boolean readFilesInDirectory = false;
        private boolean useAppDirectory;
        private FoxLogger logger = new FoxLogger.Builder()
                .setDebug(false)
                .setPruneOlderThanDays(35)
                .setSaveIntervalSeconds(300)
                .setLoggerName("DirectoryManager")
                .build();
        private Consumer<File> onInitialized = directory -> {};
        private Consumer<ReadResponse> onFileRead = response -> {};

        public Builder(File directory) {
            this.directory = directory;
        }

        public Builder setUseAppDirectory(boolean state) {
            this.useAppDirectory = state;
            return this;
        }

        public Builder setReadFilesInDirectory(boolean state) {
            this.readFilesInDirectory = state;
            return this;
        }

        public Builder setExcludeFromReading(List<String> listOfFiles) {
            this.excludeFromReading = listOfFiles;
            return this;
        }

        public Builder setExportingFiles(List<String> listOfFiles) {
            this.exportingFiles = listOfFiles;
            return this;
        }

        public Builder setLogger(FoxLogger logger) {
            this.logger = logger;
            return this;
        }

        public Builder setAllowedExtensions(List<String> allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
            return this;
        }

        public Builder onInitialized(Consumer<File> response) {
            this.onInitialized = response;
            return this;
        }

        public Builder onFileRead(Consumer<ReadResponse> response) {
            this.onFileRead = response;
            return this;
        }

        public DirectoryManager build() {
            return new DirectoryManager(this);
        }
    }

    private final Builder data;

    private DirectoryManager(Builder builder) {
        this.data = builder;

        if (data.isUseAppDirectory()) {
            data.directory = new File(System.getProperty("user.dir"));
        }

        if (builder.getDirectory() == null) {
            data.logger.warn("Directory is not set properly using project root dir");
            data.directory = new File(System.getProperty("user.dir"));
        }

        if (!builder.getDirectory().exists()) {
            data.logger.warn("Directory doesn't exists, creating a new directory!");
            if (builder.getDirectory().mkdirs()) {
                data.logger.info("New directories created!");
            } else {
                data.logger.info("Failed to create new directories, aborting...");
                return;
            }
        }

        if (!builder.getDirectory().isDirectory()) {
            data.logger.error("Selected directory path is not a valid directory!");
            return;
        }

        for (String fileName : data.getExportingFiles()) {
            File name = new File(fileName);
            File file = new File(data.directory, name.getName());

            if (!file.exists()) {
                try {
                    if (FileUtils.exportResource(file, fileName)) {
                        data.logger.info(file.getName() + " Created!");
                    }
                } catch (ExportException e) {
                    data.logger.error(e.getMessage() + " | " + e.getCause());
                }
            }
        }

        data.onInitialized.accept(data.directory);
        if (data.isReadFilesInDirectory()) {
            readAllFiles(data.onFileRead);
        }
    }

    public void reload() {
        if (data.isReadFilesInDirectory()) {
            readAllFiles(data.onFileRead);
        }
    }

    private void readAllFiles(Consumer<ReadResponse> file) {

        File[] fileArray = data.directory.listFiles();
        if (fileArray == null) {
            data.logger.error("Invalid Directory!");
            return;
        }

        if (fileArray.length > 0) {
            for (File entry : fileArray) {
                if (data.getExcludeFromReading().contains(entry.getName())) continue;
                if (!entry.isFile()) continue;

                if (!data.getAllowedExtensions().isEmpty()) {
                    if (!data.getAllowedExtensions().contains(FileUtils.getExt(entry))) {
                        continue;
                    }
                }

                String logName = "@";
                File parentFile = entry.getParentFile();
                if (parentFile != null && parentFile.isDirectory()) {
                    logName = parentFile.getName() + "/@";
                }

                new FileManager.Builder(entry)
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
