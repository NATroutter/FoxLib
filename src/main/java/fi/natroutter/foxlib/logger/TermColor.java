package fi.natroutter.foxlib.logger;

public class TermColor {

    //TODO add more colors

    public static String blue() {return blue(false);}
    public static String blue(boolean minecraft) {
        return (minecraft ? "§9" : "\u001B[36m");
    }

    public static String green() {return green(false);}
    public static String green(boolean minecraft) {
        return (minecraft ? "§a" : "\u001B[32m");
    }

    public static String red() {return red(false);}
    public static String red(boolean minecraft) {
        return (minecraft ? "§c" : "\u001B[31m");
    }

    public static String yellow() {return yellow(false);}
    public static String yellow(boolean minecraft) {
        return (minecraft ? "§e" : "\u001B[33m");
    }

    public static String reset() {return reset(false);}
    public static String reset(boolean minecraft) {
        return (minecraft ? "§r" : "\u001B[0m");
    }

}
