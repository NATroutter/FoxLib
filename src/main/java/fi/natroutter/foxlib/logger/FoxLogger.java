package fi.natroutter.foxlib.logger;

import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.files.ReadResponse;
import fi.natroutter.foxlib.files.WriteResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public class FoxLogger {

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class Builder {
        private int saveIntervalSeconds = 600;
        private int pruneOlderThanDays = 10;
        private boolean saveLogs = true;
        private boolean consoleLog = true;
        private boolean debug = false;
        private String timeFormat = "dd.MM.yyyy-HH:mm:ss";
        private String timeZone = "Europe/Helsinki";
        private File dataFolder = null;
        private String parentFolder = null;
        private String logNameSuffix = "Log";
        private LogDateFormat logDateFormat = LogDateFormat.MONTH_DAY_YEAR;
        private boolean useColors = true;
        private boolean isMinecraft = false;
        private boolean useTimeStamp = true;
        private String loggerName = "FoxLogger";
        private Consumer<String> printter = System.out::println;

        public Builder setLoggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public Builder setUseTimeStamp(boolean useTimeStamp) {
            this.useTimeStamp = useTimeStamp;
            return this;
        }

        public Builder setDataFolder(File dataFolder) {
            this.dataFolder = dataFolder;
            return this;
        }

        public Builder setParentFolder(String parentFolder) {
            this.parentFolder = parentFolder;
            return this;
        }
        public Builder setLogNameSuffix(String suffix) {
            this.logNameSuffix = suffix;
            return this;
        }
        public Builder setLogFormat(LogDateFormat format) {
            this.logDateFormat = format;
            return this;
        }
        public Builder setPrintter(Consumer<String> printter) {
            this.printter = printter;
            return this;
        }

        public Builder setIsMinecraft(boolean isMinecraft) {
            this.isMinecraft = isMinecraft;
            return this;
        }

        public Builder setSaveIntervalSeconds(int saveIntervalSeconds) {
            this.saveIntervalSeconds = saveIntervalSeconds;
            return this;
        }

        public Builder setPruneOlderThanDays(int days) {
            this.pruneOlderThanDays = days;
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

        public Builder setTimeZone(String timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public Builder setUseColors(boolean useColors) {
            this.useColors = useColors;
            return this;
        }

        public Builder setTimeFormat(String timeFormat) {
            this.timeFormat = timeFormat;
            return this;
        }

        public FoxLogger build() {return new FoxLogger(this);}
    }

    private List<String> entries = new ArrayList<>();
    private File logFolder;

    private Builder args;

    private final String BLUE;
    private final String GREEN;
    private final String RED;
    private final String YELLOW;
    private final String RESET;

    private FoxLogger(Builder builder) {
        this.args = builder;

        BLUE = args.isUseColors() ? (args.isMinecraft() ? "§9" : "\u001B[36m") : "";
        GREEN = args.isUseColors() ? (args.isMinecraft() ? "§a" : "\u001B[32m") : "";
        RED = args.isUseColors() ? (args.isMinecraft() ? "§c" : "\u001B[31m") : "";
        YELLOW = args.isUseColors() ? (args.isMinecraft() ? "§e" : "\u001B[33m") : "";
        RESET = args.isUseColors() ? (args.isMinecraft() ? "§r" : "\u001B[0m") : "";

        if (builder.getDataFolder() != null) {
            logFolder = Paths.get(builder.getDataFolder().getAbsolutePath(), "logs").toFile();
        } else {
            if (builder.getParentFolder() != null) {
                logFolder = Paths.get(System.getProperty("user.dir"), builder.getParentFolder(), "logs").toFile();
            } else {
                logFolder = new File(System.getProperty("user.dir"), "logs");
            }
        }

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

    public void log(String msg) {
        if (args.isUseColors()) { msg = msg.replace("\n", "\n" + BLUE); }
        if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][LOG] " + msg);}
        console(BLUE + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + msg + RESET);
    }
    public void info(String msg) {
        if (args.isUseColors()) { msg = msg.replace("\n", "\n" + GREEN); }
        if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][INFO] " + msg);}
        console(GREEN + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + "[INFO] " + msg + RESET);
    }
    public void error(String msg) {
        if (args.isUseColors()) { msg = msg.replace("\n", "\n" + RED); }
        if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][ERROR] " + msg);}
        console(RED + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + "[ERROR] " + msg + RESET);
    }
    public void warn(String msg) {
        if (args.isUseColors()) { msg = msg.replace("\n", "\n" + YELLOW); }
        if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][WARN] " + msg);}
        console(YELLOW + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + "[WARN] " + msg + RESET);
    }
    private void console(String msg) {
        if(args.isConsoleLog()) {
            args.getPrintter().accept(msg);
        }
    }
    private void debug(String msg) {
        if(args.isDebug()) {
            if (args.isUseColors()) { msg = msg.replace("\n", "\n" + BLUE); }
            if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][DEBUG] " + msg);}
            console(BLUE + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + msg + RESET);
        }
    }
    private String timeStamp() {
        ZoneId zone = ZoneId.of(args.getTimeZone());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(args.getTimeFormat());
        formatter.withZone(zone);
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
                    debug("File deleted : " + file.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        debug("Pruned "+pruneCount+" old log files!");
    }

    private void save() {
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
        ZonedDateTime now = ZonedDateTime.now();
        String fileName = args.getLogNameSuffix() + "_";

        switch (args.getLogDateFormat()) {
            case MONTH_DAY_YEAR -> fileName += now.getMonthValue() + "-" + now.getDayOfMonth() + "-" + now.getYear() + ".log";
            case DAY_MONTH_YEAR -> fileName += now.getDayOfMonth() + "-" + now.getMonthValue() + "-" + now.getYear() + ".log";
        }

        File saveTo = new File(logFolder, fileName);

        StringBuilder fullEntry = new StringBuilder();
        for (String entry : entries) {
            fullEntry.append(entry).append(System.lineSeparator());
        }
        String oldContent = "";
        if (saveTo.exists()) {
            ReadResponse read = FoxLib.readFile(saveTo);
            if (read.success()) {
                oldContent = read.content();
            } else {
                debug("Cant read log file! : " + read.message());
                return;
            }
        }
        WriteResponse write = FoxLib.writeFile(saveTo, oldContent + fullEntry);
        if (write.success()) {
            entries.clear();
            debug("Log file saved!");
            return;
        }
        debug("Failed to write log file!");
    }
}