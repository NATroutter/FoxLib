package fi.natroutter.foxlib.files;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.*;
import java.util.function.Consumer;


public class FileManager {

    @Getter @AllArgsConstructor
    public static class Builder {
        private File file;
        private String fileNameInLogs = "@";
        private String resourceFile = null;
        private boolean exportResource = true;
        private boolean useAppDirectory = false;
        private FoxLogger logger = new FoxLogger.Builder()
                .setDebug(false)
                .setPruneOlderThanDays(35)
                .setSaveIntervalSeconds(300)
                .setLoggerName("FileManager")
                .build();
        private Runnable onFolderCreation = () -> {};
        private Runnable onFileCreation = () -> {};
        private Consumer<ReadResponse> onInitialized = file -> {};
        private Consumer<ReadResponse> onReload = file -> {};

        public Builder(File file) {
            this.file = file;
        }

        public Builder setFileNameInLogs(String format) {
            this.fileNameInLogs = format;
            return this;
        }

        public Builder setResourceFile(String directory) {
            this.resourceFile = directory;
            return this;
        }

        public Builder setUseAppDirectory(boolean state) {
            this.useAppDirectory = state;
            return this;
        }
        public Builder setExportResource(boolean exportResource) {
            this.exportResource = exportResource;
            return this;
        }

        public Builder setLogger(FoxLogger logger) {
            this.logger = logger;
            return this;
        }

        public Builder onFolderCreation(Runnable runnable) {
            this.onFolderCreation = runnable;
            return this;
        }
        public Builder onFileCreation(Runnable runnable) {
            this.onFileCreation = runnable;
            return this;
        }
        public Builder onInitialized(Consumer<ReadResponse> response) {
            this.onInitialized = response;
            return this;
        }

        public Builder onReload(Consumer<ReadResponse> response) {
            this.onReload = response;
            return this;
        }

        public FileManager build() {
            return new FileManager(this);
        }
    }

    private final Builder data;
    private String FileContent;

    private FileManager(Builder builder) {
        this.data = builder;

        if (data.getFile() == null) {
            data.logger.warn("File is not set, skipping operation.");
            return;
        }

        if (data.getFile().isDirectory()) {
            data.logger.error("File not found: a directory was provided instead of a file.");
            return;
        }

        if (data.isUseAppDirectory()) {
            data.file = new File(System.getProperty("user.dir"), data.file.getPath());
        }

        File fileFolder = new File(data.file.getParent());

        if (!fileFolder.exists()) {
            if (fileFolder.mkdirs()) {
                data.onFolderCreation.run();
            }
        }

        if (!data.file.exists() && data.isExportResource()) {
            try {
                FoxLib.println("debug: export-> " + data.file.toString() + " | " + data.file.getName());

                boolean exportResult;

                if (data.resourceFile != null) {
                    exportResult = FileUtils.exportResource(data.file, data.resourceFile);
                } else {
                    exportResult = FileUtils.exportResource(data.file);
                }
                if (exportResult) {
                    data.logger.info(name(data.getFile().getName()) + " Created!");
                    data.onFileCreation.run();
                } else {return;}
            } catch (ExportException e) {
                data.logger.error(e.getMessage() + " | " + e.getCause());
                return;
            }
        }


        //load file
        ReadResponse response = FileUtils.readFile(data.file);
        data.getOnInitialized().accept(response);

        FileContent = response.content();
        if (FileContent == null) {
            data.logger.error(name(data.getFile().getName()) + " Failed to Loaded!");
            return;
        }
        data.logger.info(name(data.getFile().getName()) + " Loaded!");
    }

    private String name(Object name) {
        return data.getFileNameInLogs().replace("@", name.toString());
    }

    public void reload() {
        ReadResponse response = FileUtils.readFile(data.file);
        data.getOnReload().accept(response);

        FileContent = response.content();
        if (FileContent == null) {
            data.logger.error(name(data.getFile().getName()) + " Failed to Loaded!");
            return;
        }
        data.logger.info(name(data.getFile().getName()) + " Loaded!");
    }

    public String get() { return FileContent; }

    public void save(String content) {
        FileUtils.writeFile(data.file, content);
    }

}
