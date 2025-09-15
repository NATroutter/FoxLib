package fi.natroutter.foxlib.logger.types;

public class LogReason implements ILogData {

    private final String reason;

    public LogReason(String reason) {
        this.reason = reason;
    }
    public LogReason(Throwable error) {
        this.reason = error.getMessage();
    }

    @Override
    public String key() {
        return "Reason";
    }

    @Override
    public Object data() {
        return reason;
    }
}
