package fi.natroutter.foxlib.logger.types;

public class LogData implements ILogData {

    private final String key;
    private final Object data;

    public LogData(String key, Object data) {
        this.key = key;
        if (data instanceof ILogData log) {
            this.data = log.data();
        } else {
            this.data = data;
        }
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Object data() {
        return data;
    }
}
