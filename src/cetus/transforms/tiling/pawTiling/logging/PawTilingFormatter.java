package cetus.transforms.tiling.pawTiling.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class PawTilingFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        return String.format("[%s] %s%n", record.getLoggerName(), record.getMessage());
    }
}
