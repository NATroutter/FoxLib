package fi.natroutter.foxlib.logger;

import fi.natroutter.foxlib.files.FileUtils;
import fi.natroutter.foxlib.files.WriteResponse;
import fi.natroutter.foxlib.logger.types.ILogData;
import fi.natroutter.foxlib.logger.types.LogLevel;
import fi.natroutter.foxlib.utilities.TermColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    /**
     * Lines waiting to be written.
     *
     * <p>Guarded by its own monitor rather than made concurrent, because {@link #save()} needs
     * to take everything and clear it as one step: a concurrent list would let a line arriving
     * between the read and the clear be dropped. Every append is short and uncontended, so the
     * lock costs nothing worth measuring.
     */
    private final List<String> entries = new ArrayList<>();
    private File logFolder;

    /**
     * Largest a single log file grows before the next save starts a new one.
     *
     * <p>Age was the only bound, which is the wrong one on its own: a link flapping once a
     * second produces a hundred thousand lines a day, and a volume fills long before anything
     * in it is thirty-five days old. Rolled rather than truncated, so nothing already written
     * is lost.
     */
    private static final long MAX_LOG_BYTES = 32L * 1024 * 1024;

    /** Runs the periodic save and prune. Null when saving is off. */
    private ScheduledExecutorService saver;

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
            // A ScheduledExecutorService rather than a Timer, and the body wrapped, because
            // either one stops running a task for good once it throws. That used to mean a
            // single ConcurrentModificationException in save() silently ended all logging and
            // all pruning for the life of the process, which is the opposite of what a log
            // file is for.
            saver = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "foxlogger-save");
                thread.setDaemon(true);
                return thread;
            });
            saver.scheduleAtFixedRate(() -> {
                try {
                    save();
                    prune();
                } catch (Throwable t) {
                    // Deliberately swallowed after being shown. One failed save must cost one
                    // save, not every future one.
                    System.err.println("[" + args.getLoggerName() + "] Could not save the log: " + t);
                }
            }, 0, args.getSaveIntervalSeconds(), TimeUnit.SECONDS);
        }
    }

    public void close() {
        if (saver != null) {
            saver.shutdown();
        }
        save();
        prune();
    }

    public void log(String msg) { log(msg, false); }
    public void log(String msg, boolean silent) {
        if (onEntry != null) onEntry.accept(LogLevel.LOG,   "["+timeStamp()+"]["+args.loggerName+"] " + msg);
        if (args.isUseColors()) { msg = msg.replace("\n", "\n{BLUE}"); }
        if (args.isSaveLogs()) {
            synchronized (entries) {
                entries.add("[" + timeStamp() + "][LOG] " + msg);
            }
        }
        if (!silent) console("{BLUE}" + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + msg + "{RESET}");
    }

    public void info(String msg) { info(msg, false); }
    public void info(String msg, boolean silent) {
        if (onEntry != null) onEntry.accept(LogLevel.INFO,  "["+timeStamp()+"]["+args.loggerName+"][INFO] " + msg);
        if (args.isUseColors()) { msg = msg.replace("\n", "\n{GREEN}"); }
        if (args.isSaveLogs()) {
            synchronized (entries) {
                entries.add("[" + timeStamp() + "][INFO] " + msg);
            }
        }
        if (!silent) console("{GREEN}" + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + "[INFO] " + msg + "{RESET}");
    }

    public void error(String msg) { error(msg, false); }
    public void error(String msg, boolean silent) {
        if (onEntry != null) onEntry.accept(LogLevel.ERROR, "["+timeStamp()+"]["+args.loggerName+"][ERROR] " + msg);
        if (args.isUseColors()) { msg = msg.replace("\n", "\n{RED}"); }
        if (args.isSaveLogs()) {
            synchronized (entries) {
                entries.add("[" + timeStamp() + "][ERROR] " + msg);
            }
        }
        if (!silent) console("{RED}" + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + "[ERROR] " + msg + "{RESET}");
    }

    public void fatal(String msg) { fatal(msg, false); }
    public void fatal(String msg, boolean silent) {
        if (onEntry != null) onEntry.accept(LogLevel.FATAL, "["+timeStamp()+"]["+args.loggerName+"][FATAL] " + msg.toUpperCase());
        if (args.isUseColors()) { msg = msg.replace("\n", "\n{RED}"); }
        if (args.isSaveLogs()) {
            synchronized (entries) {
                entries.add("[" + timeStamp() + "][FATAL] " + msg.toUpperCase());
            }
        }
        if (!silent) console("\n{RED}" + (args.isUseTimeStamp() ? "["+timeStamp()+"]" : "") + "["+args.loggerName+"]" + "[FATAL] " + msg.toUpperCase() + "{RESET}\n");
    }

    public void warn(String msg) { warn(msg, false); }
    public void warn(String msg, boolean silent) {
        if (onEntry != null) onEntry.accept(LogLevel.WARN,  "["+timeStamp()+"]["+args.loggerName+"][WARN] " + msg);
        if (args.isUseColors()) { msg = msg.replace("\n", "\n{YELLOW}"); }
        if (args.isSaveLogs()) {
            synchronized (entries) {
                entries.add("[" + timeStamp() + "][WARN] " + msg);
            }
        }
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
            if (args.isSaveLogs()) {
                synchronized (entries) {
                    entries.add("[" + timeStamp() + "][DEBUG] " + msg);
                }
            }
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

        if (args.getPruneOlderThanDays() <= 0) {
            return;
        }

        // The same order save() wrote the name in. Reading it back with a fixed day-month-year
        // pattern, as this used to, turned the default month-day-year name "Log_9-3-2026.log"
        // (3 September) into 9 March, which is older than any retention window — so the file was
        // deleted the moment it was written, every save, on every day whose month fits in a day.
        DateTimeFormatter nameFormat = switch (args.getLogDateFormat()) {
            case MONTH_DAY_YEAR -> DateTimeFormatter.ofPattern("M-d-yyyy");
            case DAY_MONTH_YEAR -> DateTimeFormatter.ofPattern("d-M-yyyy");
        };
        // The clock save() names files by, so a file written today is never "older" than today.
        LocalDate cutoff = LocalDate.now().minusDays(args.getPruneOlderThanDays());

        int pruneCount = 0;
        File[] files = logFolder.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {continue;}
            if (!file.getName().endsWith(".log")) {continue;}

            String fileName = file.getName();
            String filenameNoExt = fileName.substring(0, fileName.length() - 4);
            String[] parts = filenameNoExt.split("_");
            if (parts.length < 2) {continue;}

            // A rolled part is "9-4-2026.2"; the date is what precedes the first dot.
            String datePart = parts[parts.length - 1];
            int dot = datePart.indexOf('.');
            if (dot >= 0) {
                datePart = datePart.substring(0, dot);
            }

            // Per file, so one name this logger did not write — or wrote under another date
            // format — is left alone rather than stopping the sweep for every file after it.
            LocalDate date;
            try {
                date = LocalDate.parse(datePart, nameFormat);
            } catch (DateTimeParseException e) {
                continue;
            }

            if (date.isBefore(cutoff)) {
                if (file.delete()) {
                    pruneCount++;
                    debug("File deleted : " + file.getName());
                }
            }
        }
        debug("Pruned "+pruneCount+" old log files!");
    }

    /**
     * Returns the file to write to, which is the next numbered part when the current one is full.
     *
     * @param base today's log file
     * @return {@code base}, or {@code Log_9-4-2026.2.log} and so on once it is over the cap
     */
    private File rolled(File base) {
        if (!base.exists() || base.length() < MAX_LOG_BYTES) {
            return base;
        }
        String name = base.getName().substring(0, base.getName().length() - 4);
        for (int part = 2; part < 1000; part++) {
            File candidate = new File(logFolder, name + "." + part + ".log");
            if (!candidate.exists() || candidate.length() < MAX_LOG_BYTES) {
                return candidate;
            }
        }
        return base;
    }

    private void save() {
        List<String> pending;
        synchronized (entries) {
            if (entries.isEmpty()) {
                return;
            }
            pending = new ArrayList<>(entries);
            entries.clear();
        }

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
        saveTo = rolled(saveTo);

        StringBuilder fullEntry = new StringBuilder();
        for (String entry : pending) {
            fullEntry.append(entry).append(System.lineSeparator());
        }

        WriteResponse write = FileUtils.appendFile(saveTo, fullEntry.toString());
        if (write.success()) {
            debug("Log file saved!");
            return;
        }
        synchronized (entries) {
            entries.addAll(0, pending);
        }
        debug("Failed to write log file!");
    }
}