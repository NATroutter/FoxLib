package fi.natroutter.foxlib.logger;

public class TermColor {

    public static String black() {return black(false);}
    public static String black(boolean minecraft) {
        return (minecraft ? "§0" : "\u001B[30m");
    }

    public static String red() {return red(false);}
    public static String red(boolean minecraft) {
        return (minecraft ? "§c" : "\u001B[31m");
    }

    public static String green() {return green(false);}
    public static String green(boolean minecraft) {
        return (minecraft ? "§a" : "\u001B[32m");
    }

    public static String yellow() {return yellow(false);}
    public static String yellow(boolean minecraft) {
        return (minecraft ? "§e" : "\u001B[33m");
    }

    public static String blue() {return blue(false);}
    public static String blue(boolean minecraft) {
        return (minecraft ? "§9" : "\u001B[34m");
    }

    public static String magenta() {return magenta(false);}
    public static String magenta(boolean minecraft) {
        return (minecraft ? "§d" : "\u001B[35m");
    }

    public static String cyan() {return cyan(false);}
    public static String cyan(boolean minecraft) {
        return (minecraft ? "§b" : "\u001B[36m");
    }

    public static String white() {return white(false);}
    public static String white(boolean minecraft) {
        return (minecraft ? "§f" : "\u001B[37m");
    }

    public static String reset() {return reset(false);}
    public static String reset(boolean minecraft) {
        return (minecraft ? "§r" : "\u001B[0m");
    }

}
