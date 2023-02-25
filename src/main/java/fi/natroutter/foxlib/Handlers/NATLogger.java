package fi.natroutter.foxlib.Handlers;

import fi.natroutter.foxlib.data.FileResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class NATLogger {

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class Builder {
        private int saveIntervalSeconds = 600;
        private int pruneOlderThanDays = 10;
        private boolean saveLogs = true;
        private boolean consoleLog = true;
        private boolean debug = false;
        private String timeFormat = "dd.MM.yyyy-HH:mm:ss";
        private File dataFolder = null;

        public Builder setDataFolder(File dataFolder) {
            this.dataFolder = new File(dataFolder, "logs");
            return this;
        }

        public Builder setSaveIntervalSeconds(int saveIntervalSeconds) {
            this.saveIntervalSeconds = saveIntervalSeconds;
            return this;
        }

        public Builder setPruneOlderThanDays(int seconds) {
            this.pruneOlderThanDays = seconds;
            return this;
        }

        public Builder setSaveLogs(boolean saveLogs) {
            this.saveLogs = saveLogs;
            return this;
        }

        public Builder setConsoleLog(boolean consoleLog) {
            this.consoleLog = consoleLog;
            return this;
        }

        public Builder setDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder setTimeFormat(String timeFormat) {
            this.timeFormat = timeFormat;
            return this;
        }

        public NATLogger build() {return new NATLogger(this);}
    }

    private List<String> entries = new ArrayList<>();
    private File logFolder;

    private Builder args;

    private NATLogger(Builder builder) {
        this.args = builder;
        logFolder = builder.getDataFolder() != null ? builder.getDataFolder() : new File(System.getProperty("user.dir"), "logs");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }

        //Timer
        if (args.isSaveLogs()) {
            new Timer().schedule(new TimerTask() {
                public void run() {
                    save();
                    prune();
                }
            }, 0, args.getSaveIntervalSeconds() * 1000L);
        }
    }

    public void close() {
        save();
        prune();
    }

    public void info(String msg) {
        if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][INFO] " + msg);}
        console("\u001B[32m["+timeStamp()+"][INFO] " + msg + "\u001B[0m");
    }
    public void error(String msg) {
        if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][ERROR] " + msg);}
        console("\u001B[31m["+timeStamp()+"][ERROR] " + msg + "\u001B[0m");
    }
    public void warn(String msg) {
        if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][WARN] " + msg);}
        console("\u001B[33m["+timeStamp()+"][WARN] " + msg + "\u001B[0m");
    }
    private void console(String msg) {
        if(args.isConsoleLog()) {System.out.println(msg);}
    }
    private void debug(String msg) {
        if(args.isDebug()) {System.out.println(msg);}
    }
    private String timeStamp() {
        ZoneId helsinki = ZoneId.of("Europe/Helsinki");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(args.getTimeFormat());
        formatter.withZone(helsinki);
        return LocalDateTime.now().format(formatter);
    }

    private void prune() {
        if (!logFolder.exists()) {
            logFolder.mkdirs();
            return;
        }

        int pruneCount = 0;
        try {
            for (File file : logFolder.listFiles()) {
                if (file.isDirectory()) {continue;}
                if (!file.getName().endsWith(".log")) {continue;}

                String fileName = file.getName();
                String filenameNoExt = fileName.substring(0, fileName.length() - 4);
                String dateString = filenameNoExt.split("_")[1];

                Date date = new SimpleDateFormat("dd-MM-yyyy").parse(dateString);
                ZonedDateTime Ago = ZonedDateTime.now().plusDays(-args.getPruneOlderThanDays());
                if (date.toInstant().isBefore(Ago.toInstant())) {
                    file.delete();
                    pruneCount++;
                    debug("[Info/NATLogger] File deleted : " + file.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        debug("[Info/NATLogger] Pruned "+pruneCount+" old log files!");
    }

    private void save() {
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
        ZonedDateTime now = ZonedDateTime.now();
        String fileName = "Log_" + now.getDayOfMonth() + "-" + now.getMonthValue() + "-" + now.getYear() + ".log";
        File saveTo = new File(logFolder, fileName);

        StringBuilder fullEntry = new StringBuilder();
        for (String entry : entries) {
            fullEntry.append(entry).append(System.lineSeparator());
        }
        String oldContent = "";
        if (saveTo.exists()) {
            FileResponse read = FileUtils.readFile(saveTo);
            if (read.success()) {
                oldContent = read.content();
            } else {
                debug("[Error/NATLogger] Cant read log file! : " + read.status());
                return;
            }
        }
        FileResponse write = FileUtils.writeFile(saveTo, oldContent + fullEntry);
        if (write.success()) {
            entries.clear();
            debug("[Info/NATLogger] Log file saved!");
            return;
        }
        debug("[Error/NATLogger] Failed to write log file!");
    }
}