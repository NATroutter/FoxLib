package fi.natroutter.foxlib.Handlers;

import fi.natroutter.foxlib.data.FileResponse;

import java.io.*;

public class FileUtils {

    public static FileResponse readFile(File file) {
        try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr)) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line).append(System.lineSeparator());
                line = br.readLine();
            }
            return new FileResponse(true, "OK", sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return new FileResponse(false, e.getMessage(), null);
        }
    }

    public static FileResponse writeFile(File file, String Content) {
        try(FileWriter fw = new FileWriter(file); BufferedWriter bw = new BufferedWriter(fw);) {
            if (!file.exists()) {
                file.createNewFile();
            }
            bw.write(Content);
            return new FileResponse(true, "OK", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new FileResponse(false, e.getMessage(), null);
        }
    }

}
