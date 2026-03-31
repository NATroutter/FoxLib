package fi.natroutter.foxlib.logger;

import fi.natroutter.foxlib.files.FileUtils;
import fi.natroutter.foxlib.files.ReadResponse;
import fi.natroutter.foxlib.files.WriteResponse;
import fi.natroutter.foxlib.logger.types.ILogData;
import fi.natroutter.foxlib.logger.types.LogLevel;
import fi.natroutter.foxlib.utilities.TermColor;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
        private boolean useTimeStamp = true;
        private String loggerName = "FoxLogger";
        private Consumer<String> printter = System.out::println;
        private BiConsumer<LogLevel, String> onEntry = null;

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

        /**
         * Registers a callback invoked for every log entry before color processing.
         * The first argument is the {@link LogLevel}, the second is the formatted message
         * {@code [timestamp][loggerName][LEVEL] text} with clean (non-colored) content.
         */
        public Builder setOnEntry(BiConsumer<LogLevel, String> onEntry) {
            this.onEntry = onEntry;
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
    private final BiConsumer<LogLevel, String> onEntry;


    private FoxLogger(Builder builder) {
        this.args = builder;
        this.onEntry = builder.onEntry;

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

    public void log(String msg) { log(msg, false); }
    public void log(String msg, boolean silent) {
        if (onEntry != null) onEntry.accept(LogLevel.LOG,   "["+timeStamp()+"]["+args.loggerName+"] " + msg);
        if (args.isUseColors()) { msg = msg.replace("\n", "\n{BLUE}"); }
        if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][LOG] " + msg);}
        if (!silent) console("{BLUE}" + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + msg + "{RESET}");
    }

    public void info(String msg) { info(msg, false); }
    public void info(String msg, boolean silent) {
        if (onEntry != null) onEntry.accept(LogLevel.INFO,  "["+timeStamp()+"]["+args.loggerName+"][INFO] " + msg);
        if (args.isUseColors()) { msg = msg.replace("\n", "\n{GREEN}"); }
        if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][INFO] " + msg);}
        if (!silent) console("{GREEN}" + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + "[INFO] " + msg + "{RESET}");
    }

    public void error(String msg) { error(msg, false); }
    public void error(String msg, boolean silent) {
        if (onEntry != null) onEntry.accept(LogLevel.ERROR, "["+timeStamp()+"]["+args.loggerName+"][ERROR] " + msg);
        if (args.isUseColors()) { msg = msg.replace("\n", "\n{RED}"); }
        if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][ERROR] " + msg);}
        if (!silent) console("{RED}" + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + "[ERROR] " + msg + "{RESET}");
    }

    public void fatal(String msg) { fatal(msg, false); }
    public void fatal(String msg, boolean silent) {
        if (onEntry != null) onEntry.accept(LogLevel.FATAL, "["+timeStamp()+"]["+args.loggerName+"][FATAL] " + msg.toUpperCase());
        if (args.isUseColors()) { msg = msg.replace("\n", "\n{RED}"); }
        if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][FATAL] " + msg.toUpperCase());}
        if (!silent) console("\n{RED}" + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + "[FATAL] " + msg.toUpperCase() + "{RESET}\n");
    }

    public void warn(String msg) { warn(msg, false); }
    public void warn(String msg, boolean silent) {
        if (onEntry != null) onEntry.accept(LogLevel.WARN,  "["+timeStamp()+"]["+args.loggerName+"][WARN] " + msg);
        if (args.isUseColors()) { msg = msg.replace("\n", "\n{YELLOW}"); }
        if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][WARN] " + msg);}
        if (!silent) console("{YELLOW}" + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + "[WARN] " + msg + "{RESET}");
    }

    public void log(LogLevel level, String msg) { log(level, msg, false); }
    public void log(LogLevel level, String msg, boolean silent) {
        switch (level) {
            case INFO -> info(msg, silent);
            case ERROR -> error(msg, silent);
            case FATAL -> fatal(msg, silent);
            case WARN -> warn(msg, silent);
            default -> log(msg, silent);
        }
    }

    //Loggers with (message and throwable)
    public void log(String msg, Throwable throwable) { log(msg, throwable, false); }
    public void log(String msg, Throwable throwable, boolean silent) {
        log(msg + " : " + throwable.getMessage(), silent);
    }
    public void log(LogLevel level, String msg, Throwable throwable) { log(level, msg, throwable, false); }
    public void log(LogLevel level, String msg, Throwable throwable, boolean silent) {
        log(level, msg + " : " + throwable.getMessage(), silent);
    }

    public void info(String msg, Throwable throwable) { info(msg, throwable, false); }
    public void info(String msg, Throwable throwable, boolean silent) {
        info(msg + " : " + throwable.getMessage(), silent);
    }
    public void error(String msg, Throwable throwable) { error(msg, throwable, false); }
    public void error(String msg, Throwable throwable, boolean silent) {
        error(msg + " : " + throwable.getMessage(), silent);
    }
    public void fatal(String msg, Throwable throwable) { fatal(msg, throwable, false); }
    public void fatal(String msg, Throwable throwable, boolean silent) {
        fatal(msg + " : " + throwable.getMessage(), silent);
    }
    public void warn(String msg, Throwable throwable) { warn(msg, throwable, false); }
    public void warn(String msg, Throwable throwable, boolean silent) {
        warn(msg + " : " + throwable.getMessage(), silent);
    }


    private String getDataBlock(ILogData... data) {
        return Arrays.stream(data).map(d->d.key() + "=\"" + d.data().toString() + "\"").collect(Collectors.joining(", "));
    }

    //Loggers with (message and data)
    public void log(String msg, ILogData... data) { log(msg, false, data); }
    public void log(String msg, boolean silent, ILogData... data) {
        log(msg + " ["+getDataBlock(data)+"]", silent);
    }
    public void log(LogLevel level, String msg, ILogData... data) { log(level, msg, false, data); }
    public void log(LogLevel level, String msg, boolean silent, ILogData... data) {
        log(level, msg + " ["+getDataBlock(data)+"]", silent);
    }
    public void info(String msg, ILogData... data) { info(msg, false, data); }
    public void info(String msg, boolean silent, ILogData... data) {
        info(msg + " ["+getDataBlock(data)+"]", silent);
    }
    public void error(String msg, ILogData... data) { error(msg, false, data); }
    public void error(String msg, boolean silent, ILogData... data) {
        error(msg + " ["+getDataBlock(data)+"]", silent);
    }
    public void fatal(String msg, ILogData... data) { fatal(msg, false, data); }
    public void fatal(String msg, boolean silent, ILogData... data) {
        fatal(msg + " ["+getDataBlock(data)+"]", silent);
    }
    public void warn(String msg, ILogData... data) { warn(msg, false, data); }
    public void warn(String msg, boolean silent, ILogData... data) {
        warn(msg + " ["+getDataBlock(data)+"]", silent);
    }

    //Loggers with (message, throwable and data)
    public void log(String msg, Throwable throwable, ILogData... data) { log(msg, throwable, false, data); }
    public void log(String msg, Throwable throwable, boolean silent, ILogData... data) {
        log(msg + " ["+getDataBlock(data)+"] : " + throwable.getMessage(), silent);
    }
    public void log(String msg, LogLevel level, Throwable throwable, ILogData... data) { log(msg, level, throwable, false, data); }
    public void log(String msg, LogLevel level, Throwable throwable, boolean silent, ILogData... data) {
        log(level, msg + " ["+getDataBlock(data)+"] : " + throwable.getMessage(), silent);
    }
    public void info(String msg, Throwable throwable, ILogData... data) { info(msg, throwable, false, data); }
    public void info(String msg, Throwable throwable, boolean silent, ILogData... data) {
        info(msg + " ["+getDataBlock(data)+"] : " + throwable.getMessage(), silent);
    }
    public void error(String msg, Throwable throwable, ILogData... data) { error(msg, throwable, false, data); }
    public void error(String msg, Throwable throwable, boolean silent, ILogData... data) {
        error(msg + " ["+getDataBlock(data)+"] : " + throwable.getMessage(), silent);
    }
    public void fatal(String msg, Throwable throwable, ILogData... data) { fatal(msg, throwable, false, data); }
    public void fatal(String msg, Throwable throwable, boolean silent, ILogData... data) {
        fatal(msg + " ["+getDataBlock(data)+"] : " + throwable.getMessage(), silent);
    }
    public void warn(String msg, Throwable throwable, ILogData... data) { warn(msg, throwable, false, data); }
    public void warn(String msg, Throwable throwable, boolean silent, ILogData... data) {
        warn(msg + " ["+getDataBlock(data)+"] : " + throwable.getMessage(), silent);
    }


    private void console(String msg) {
        if(args.isConsoleLog()) {
            args.getPrintter().accept(TermColor.parse(args.isUseColors(), msg));
        }
    }
    private void debug(String msg) {
        if(args.isDebug()) {
            if (args.isUseColors()) { msg = msg.replace("\n", "\n{BLUE}"); }
            if (args.isSaveLogs()) {entries.add("["+timeStamp()+"][DEBUG] " + msg);}
            console("{BLUE}" + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + msg + "{RESET}");
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
            ReadResponse read = FileUtils.readFile(saveTo);
            if (read.success()) {
                oldContent = read.content();
            } else {
                debug("Cant read log file! : " + read.message());
                return;
            }
        }
        WriteResponse write = FileUtils.writeFile(saveTo, oldContent + fullEntry);
        if (write.success()) {
            entries.clear();
            debug("Log file saved!");
            return;
        }
        debug("Failed to write log file!");
    }
}