package fi.natroutter.foxlib.files;

import fi.natroutter.foxlib.FoxLib;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.*;
import java.nio.file.Path;
import java.util.function.Consumer;


public class FileManager {

    @Getter @AllArgsConstructor
    public static class Builder {
        private String fileName;
        private String logFileNameFormat = "@";
        private boolean exportResource = true;
        private boolean loading = true;
        private File directory = null;
        private Consumer<String> errorLogger = message -> {
            System.out.println("FileManager/Error : " + message);
        };
        private Consumer<String> infoLogger = message -> {
            System.out.println("FileManager/Info : " + message);
        };
        private Consumer<ReadResponse> onInitialized = file -> {};
        private Consumer<ReadResponse> onReload = file -> {};

        public Builder(String fileName) {
            this.fileName = fileName;
        }

        public Builder setLogFileNameFormat(String format) {
            this.logFileNameFormat = format;
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

        public Builder onInitialized(Consumer<ReadResponse> response) {
            this.onInitialized = response;
            return this;
        }

        public Builder onReload(Consumer<ReadResponse> response) {
            this.onReload = response;
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

        public FileManager build() {
            return new FileManager(this);
        }
    }



    private Builder data;

    private File file;
    private File fileFolder;
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

        fileFolder = new File(file.getParent());

        if (!fileFolder.exists()) {
            fileFolder.mkdirs();
        }

        if (!file.exists() && data.isExportResource()) {
            if (exportResource(file, data.getFileName())) {
                info(name(data.getFileName()) + " Created!");
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
                error(name(data.getFileName()) + " Failed to Loaded!");
                return;
            }
            info(name(data.getFileName()) + " Loaded!");
        } else {
            data.getOnInitialized().accept(null);
        }
    }

    private String name(Object name) {
        return data.getLogFileNameFormat().replace("@", name.toString());
    }
    private void info(Object message) {
        data.getInfoLogger().accept(message.toString());
    }
    private void error(Object message) {
        data.getErrorLogger().accept(message.toString());
    }

    public void reload() {
        ReadResponse response = FoxLib.readFile(file);
        data.getOnReload().accept(response);

        FileContent = response.content();
        if (FileContent == null) {
            error(name(data.getFileName()) + " Failed to Loaded!");
            return;
        }
       info(name(data.getFileName()) + " Loaded!");
    }

    public String get() { return FileContent; }

    public void save(String data) {
        FoxLib.writeFile(file, data);
    }

    private boolean exportResource(File file, String resourceName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try(InputStream stream = classLoader.getResourceAsStream(resourceName)) {
            if(stream == null) {
                error("Failed to export resource ("+resourceName+") : File doesn't exist");
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
                error("Failed to export resource ("+resourceName+") : " + ex.getCause().getMessage());
            }
        } catch (Exception ex) {
            error("Failed to export resource ("+resourceName+") : " + ex.getCause().getMessage());
        }
        return false;
    }

}
