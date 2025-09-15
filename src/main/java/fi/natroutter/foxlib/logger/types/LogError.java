package fi.natroutter.foxlib.logger.types;

public class LogError implements ILogData {

    private final String error;

    public LogError(Throwable error) {
        this.error = error.getMessage();
    }

    @Override
    public String key() {
        return "Error";
    }

    @Override
    public Object data() {
        return error;
    }
}
