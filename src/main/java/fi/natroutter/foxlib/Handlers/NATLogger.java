package fi.natroutter.foxlib.Handlers;

public class NATLogger {

    public void info(String msg) {
        log("\u001B[32m[INFO] " + msg + "\u001B[0m");
    }

    public void error(String msg) {
        log("\u001B[31m[ERROR] " + msg + "\u001B[0m");
    }

    public void warn(String msg) {
        log("\u001B[33m[WARN] " + msg + "\u001B[0m");
    }

    public void log(String msg) {
        System.out.println(msg);
    }

}
