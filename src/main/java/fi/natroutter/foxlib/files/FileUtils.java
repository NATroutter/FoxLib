package fi.natroutter.foxlib.files;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.util.function.Consumer;

public class FileUtils {

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

    /**
     * Appends to a file, creating it if it is not there.
     *
     * <p>Distinct from {@link #writeFile}, which truncates. A log wants this one: rewriting the
     * whole file to add a line is quadratic in its size, holds two copies of it in memory, and
     * loses everything rather than the last few lines if the write is interrupted.
     *
     * @param file    the file to append to
     * @param content the text to add, which should end with a line separator
     * @return whether it was written
     */
    public static WriteResponse appendFile(File file, String content) {
        try (FileWriter fw = new FileWriter(file, true); BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(content);
            bw.flush();
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
                // An array, not a string. A single-string exec is split on whitespace, so a
                // path with a space in it becomes several arguments, and anything a caller can
                // put in the path becomes part of the command line.
                Runtime.getRuntime().exec(new String[] {"explorer.exe", "/select," + path});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[] {"open", path});
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec(new String[] {"xdg-open", path});
            }
        }
    }
}
