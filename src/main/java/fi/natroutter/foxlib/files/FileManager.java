package fi.natroutter.foxlib.files;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.*;
import java.nio.file.Path;
import java.util.function.Consumer;


public class FileManager {

    @Getter @AllArgsConstructor
    public static class Builder {
        private String fileName;
        private String fileNameInLogs = "@";
        private boolean exportResource = true;
        private boolean loading = true;
        private File directory = null;
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

        public Builder(String fileName) {
            this.fileName = fileName;
        }

        public Builder setFileNameInLogs(String format) {
            this.fileNameInLogs = format;
            return this;
        }

        public Builder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder setDirectory(File directory) {
            this.directory = directory;
            return this;
        }
        public Builder setLoading(boolean loading) {
            this.loading = loading;
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

    private final File file;
    private String FileContent;

    private FileManager(Builder builder) {
        this.data = builder;

        if (data.getFileName().startsWith("/") || data.getFileName().startsWith("\\")) {
            data.setFileName(data.getFileName().substring(1));
        }

        if (builder.getDirectory() != null) {
            file = Path.of(builder.getDirectory().toString(), data.getFileName()).toFile();
        } else {
            file = Path.of(System.getProperty("user.dir"), data.getFileName()).toFile();
        }

        File fileFolder = new File(file.getParent());

        if (!fileFolder.exists()) {
            if (fileFolder.mkdirs()) {
                data.onFolderCreation.run();
            }
        }

        if (!file.exists() && data.isExportResource()) {
            if (exportResource(file, data.getFileName())) {
                data.logger.info(name(data.getFileName()) + " Created!");
                data.onFileCreation.run();
            } else {
                return;
            }
        }


        //load file
        if (data.isLoading()) {
            ReadResponse response = FoxLib.readFile(file);
            data.getOnInitialized().accept(response);

            FileContent = response.content();
            if (FileContent == null) {
                data.logger.error(name(data.getFileName()) + " Failed to Loaded!");
                return;
            }
            data.logger.info(name(data.getFileName()) + " Loaded!");
        } else {
            data.getOnInitialized().accept(null);
        }
    }

    private String name(Object name) {
        return data.getFileNameInLogs().replace("@", name.toString());
    }

    public void reload() {
        ReadResponse response = FoxLib.readFile(file);
        data.getOnReload().accept(response);

        FileContent = response.content();
        if (FileContent == null) {
            data.logger.error(name(data.getFileName()) + " Failed to Loaded!");
            return;
        }
        data.logger.info(name(data.getFileName()) + " Loaded!");
    }

    public String get() { return FileContent; }

    public void save(String data) {
        FoxLib.writeFile(file, data);
    }

    private boolean exportResource(File file, String resourceName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try(InputStream stream = classLoader.getResourceAsStream(resourceName)) {
            if(stream == null) {
                data.logger.error("Failed to export resource ("+resourceName+") : File doesn't exist");
                return false;
            }
            try(OutputStream resStreamOut = new FileOutputStream(file)) {
                int readBytes;
                byte[] buffer = new byte[4096];
                while ((readBytes = stream.read(buffer)) > 0) {
                    resStreamOut.write(buffer, 0, readBytes);
                }
                return true;
            } catch (Exception ex) {
                data.logger.error("Failed to export resource ("+resourceName+") : " + ex.getCause().getMessage());
            }
        } catch (Exception ex) {
            data.logger.error("Failed to export resource ("+resourceName+") : " + ex.getCause().getMessage());
        }
        return false;
    }

}
