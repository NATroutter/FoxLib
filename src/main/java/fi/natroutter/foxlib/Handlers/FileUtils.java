package fi.natroutter.foxlib.Handlers;

import fi.natroutter.foxlib.data.FileResponse;

import java.io.*;
import java.nio.file.Path;

public class FileUtils {

    public static FileResponse readFile(File file) {
        try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr)) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line).append(System.lineSeparator());
                line = br.readLine();
            }
            return new FileResponse(true, file.getName(), "OK", sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return new FileResponse(false, file.getName(), e.getMessage(), null);
        }
    }

    public static FileResponse writeFile(File file, String Content) {
        try(FileWriter fw = new FileWriter(file); BufferedWriter bw = new BufferedWriter(fw);) {
            if (!file.exists()) {
                file.createNewFile();
            }
            bw.write(Content);
            return new FileResponse(true, file.getName(), "OK", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new FileResponse(false, file.getName(), e.getMessage(), null);
        }
    }

    public static String getBasename(FileResponse resp) { return getBasename(resp.name()); }
    public static String getBasename(File file) { return getBasename(file.getName()); }
    public static String getBasename(String fileName) {
        Path path = Path.of(fileName);
        String fullName = path.getFileName().toString();
        int lastDotIndex = fullName.lastIndexOf('.');
        return (lastDotIndex > 0) ? fullName.substring(0, lastDotIndex) : fullName;
    }

    public static String getExt(FileResponse resp) { return getExt(resp.name()); }
    public static String getExt(File file) { return getExt(file.getName()); }
    public static String getExt(String fileName) {
        if (!fileName.contains(".")) return fileName;
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

}
