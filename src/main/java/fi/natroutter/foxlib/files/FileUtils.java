package fi.natroutter.foxlib.files;

import fi.natroutter.foxlib.logger.FoxLogger;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.util.function.Consumer;

public class FileUtils {

    private static FoxLogger logger = new FoxLogger.Builder()
            .setDebug(false)
            .setPruneOlderThanDays(35)
            .setSaveIntervalSeconds(300)
            .setLoggerName("FileUtils")
            .build();


    public static boolean exportResource(File outputFile) throws ExportException {
        return exportResource(outputFile, outputFile.getName());
    }

    public static InputStream streamResource(String name) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader.getResourceAsStream(name);
    }

    public static boolean exportResource(File outputFile, String resourceFile) throws ExportException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try(InputStream stream = classLoader.getResourceAsStream(resourceFile)) {
            if(stream == null) {
                throw new ExportException("Failed to export resource, resource doesn't exist", outputFile, resourceFile);
            }
            try(OutputStream resStreamOut = new FileOutputStream(outputFile)) {
                int readBytes;
                byte[] buffer = new byte[8192];
                while ((readBytes = stream.read(buffer)) > 0) {
                    resStreamOut.write(buffer, 0, readBytes);
                }
                return true;
            } catch (Exception ex) {
                throw new ExportException("Failed to export resource x1", ex.getCause(), outputFile, resourceFile);
            }
        } catch (Exception ex) {
            throw new ExportException("Failed to export resource x2", ex.getCause(), outputFile, resourceFile);
        }
    }

    public static ReadResponse readFile(File file) {
        return readFile(file, (e)->{});
    }

    public static ReadResponse readFile(File file, Consumer<Float> progress) {
        try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr)) {
            StringBuilder sb = new StringBuilder();

            long fileSize = file.length();
            long bytesRead = 0;

            String line = br.readLine();

            while (line != null) {
                sb.append(line).append(System.lineSeparator());

                // Update bytes read (approximate based on line length + line separator)
                bytesRead += line.length() + System.lineSeparator().length();

                // Report progress
                if (progress != null && fileSize > 0) {
                    float progressValue = Math.min((float) bytesRead / fileSize, 1.0f);
                    progress.accept(progressValue);
                }

                line = br.readLine();
            }

            if (progress != null) {
                progress.accept(1.0f); // Ensure we report 100% completion
            }

            return new ReadResponse(true, file.getName(), "OK", sb.toString());
        } catch (Exception e) {
            return new ReadResponse(false, file.getName(), e.getMessage(), null);
        }
    }

    public static WriteResponse writeFile(File file, String content) {
        return writeFile(file,content, (e)->{});
    }

    public static WriteResponse writeFile(File file, String content, Consumer<Float> progress) {
        try (FileWriter fw = new FileWriter(file); BufferedWriter bw = new BufferedWriter(fw)) {
            if (!file.exists()) {
                file.createNewFile();
            }

            int totalLength = content.length();
            int chunkSize = Math.max(1024, totalLength / 100); // Write in chunks, at least 1KB

            for (int i = 0; i < totalLength; i += chunkSize) {
                int end = Math.min(i + chunkSize, totalLength);
                bw.write(content, i, end - i);

                // Report progress
                float progressValue = (float) end / totalLength;
                if (progress != null) {
                    progress.accept(progressValue);
                }
            }

            bw.flush();

            if (progress != null) {
                progress.accept(1.0f); // Ensure we report 100% completion
            }

            return new WriteResponse(true, file.getName(), "OK");
        } catch (Exception e) {
            return new WriteResponse(false, file.getName(), e.getMessage());
        }
    }

    public static String getBasename(ReadResponse resp) { return getBasename(resp.name()); }
    public static String getBasename(File file) { return getBasename(file.getName()); }
    public static String getBasename(String fileName) {
        Path path = Path.of(fileName);
        String fullName = path.getFileName().toString();
        int lastDotIndex = fullName.lastIndexOf('.');
        return (lastDotIndex > 0) ? fullName.substring(0, lastDotIndex) : fullName;
    }

    public static String getExt(ReadResponse resp) { return getExt(resp.name()); }
    public static String getExt(File file) { return getExt(file.getName()); }
    public static String getExt(String fileName) {
        if (!fileName.contains(".")) return fileName;
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }


    public static void openFileExplorer(File file) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file);
        } else {
            // Fallback for different OS
            String os = System.getProperty("os.name").toLowerCase();
            String path = file.getAbsolutePath();

            if (os.contains("win")) {
                Runtime.getRuntime().exec("explorer.exe /select," + path);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + path);
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec("xdg-open " + path);
            }
        }
    }
}
