package fi.natroutter.foxlib.Handlers;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.*;
import java.util.function.Consumer;

public class FileManager {

    @Getter @AllArgsConstructor
    public static class Builder {
        private String fileName;
        private String subFolder = "";
        private boolean exportResource = true;
        private File directory = null;
        private Consumer<String> errorLogger = message -> {
            System.out.println("FileManager/Error : " + message);
        };
        private Consumer<String> infoLogger = message -> {
            System.out.println("FileManager/Info : " + message);
        };

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

        public Builder setSubFolder(String subFolder) {
            this.subFolder = subFolder;
            return this;
        }

        public Builder setExportResource(boolean exportResource) {
            this.exportResource = exportResource;
            return this;
        }

        public Builder setErrorLogger(Consumer<String> errorLogger) {
            this.errorLogger = errorLogger;
            return this;
        }

        public Builder setInfoLogger(Consumer<String> infoLogger) {
            this.infoLogger = infoLogger;
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

    @Getter
    private boolean initialized = false;

    private FileManager(Builder builder) {
        this.data = builder;

        if (builder.getDirectory() != null) {
            file = new File(builder.getDirectory(), (data.getSubFolder().length() > 0 && !data.getSubFolder().isBlank() ? data.getSubFolder() + "/" : "") + data.getFileName());
        } else {
            file = new File(System.getProperty("user.dir"), (data.getSubFolder().length() > 0 && !data.getSubFolder().isBlank() ? data.getSubFolder() + "/" : "") + data.getFileName());
        }

        fileFolder = new File(file.getParent());

        if (!fileFolder.exists()) {
            fileFolder.mkdirs();
        }
        if (!file.exists()) {
            if (data.isExportResource()) {
                if (!exportResource(file, data.getFileName())) {
                    return;
                }
            } else {
                data.getErrorLogger().accept(data.getFileName() + " doesn't exists!");
                return;
            }
        }
        if (data.isExportResource()) {
            reload();
        }
    }

    public void reload() {
        FileContent = FileUtils.readFile(file).content();
        if (FileContent != null) {
            data.getInfoLogger().accept(data.getFileName() + " Loaded!");
            initialized = true;
        }
    }

    public String get() { return FileContent; }

    public void save(String data) {
    	FileUtils.writeFile(file, data);
    }

    private boolean exportResource(File file, String resourceName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try(InputStream stream = classLoader.getResourceAsStream(resourceName); OutputStream resStreamOut = new FileOutputStream(file)) {
            if(stream == null) {
                data.getErrorLogger().accept("Failed to export resource : " + resourceName);
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
