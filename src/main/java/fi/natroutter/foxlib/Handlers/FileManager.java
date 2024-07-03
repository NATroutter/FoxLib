package fi.natroutter.foxlib.Handlers;

import fi.natroutter.foxlib.data.FileResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.*;
import java.nio.file.Path;
import java.util.function.Consumer;

public class FileManager {

    @Getter @AllArgsConstructor
    public static class Builder {
        private String fileName;
        private boolean exportResource = true;
        private File directory = null;
        private Consumer<String> errorLogger = message -> {
            System.out.println("FileManager/Error : " + message);
        };
        private Consumer<String> infoLogger = message -> {
            System.out.println("FileManager/Info : " + message);
        };
        private Consumer<FileResponse> onInitialized = file -> {};
        private Consumer<FileResponse> onReload = file -> {};

        public Builder(String fileName) {
            this.fileName = fileName;
        }

        public Builder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder setDirectory(File directory) {
            this.directory = directory;
            return this;
        }

        public Builder setExportResource(boolean exportResource) {
            this.exportResource = exportResource;
            return this;
        }

        public Builder onInitialized(Consumer<FileResponse> response) {
            this.onInitialized = response;
            return this;
        }

        public Builder onReload(Consumer<FileResponse> response) {
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
        System.out.println("test: " + file);
        if (!file.exists()) {
            if (data.isExportResource()) {
                if (!exportResource(file, data.getFileName())) {
                    return;
                }
            } else {
                error(data.getFileName() + " doesn't exists!");
                return;
            }
        }

        //load file
        FileResponse response = FileUtils.readFile(file);
        data.getOnInitialized().accept(response);

        FileContent = response.fileContent();
        if (FileContent == null) {
            error(data.getFileName() + " Failed to Loaded!");
            return;
        }
        info(data.getFileName() + " Loaded!");
    }

    private void info(Object message) {
        data.getInfoLogger().accept(message.toString());
    }
    private void error(Object message) {
        data.getErrorLogger().accept(message.toString());
    }

    public void reload() {
        FileResponse response = FileUtils.readFile(file);
        data.getOnReload().accept(response);

        FileContent = response.fileContent();
        if (FileContent == null) {
            error(data.getFileName() + " Failed to Loaded!");
            return;
        }
       info(data.getFileName() + " Loaded!");
    }

    public String get() { return FileContent; }

    public void save(String data) {
    	FileUtils.writeFile(file, data);
    }

    private boolean exportResource(File file, String resourceName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try(InputStream stream = classLoader.getResourceAsStream(resourceName); OutputStream resStreamOut = new FileOutputStream(file)) {
            if(stream == null) {
                error("Failed to export resource : " + resourceName);
                return false;
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

}
