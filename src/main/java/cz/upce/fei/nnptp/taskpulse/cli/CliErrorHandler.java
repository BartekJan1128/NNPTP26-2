package cz.upce.fei.nnptp.taskpulse.cli;

import cz.upce.fei.nnptp.taskpulse.domain.exception.TaskPulseException;
import java.io.PrintStream;
import java.util.Objects;

public class CliErrorHandler {

    private final PrintStream err;

    public CliErrorHandler(PrintStream err) {
        this.err = Objects.requireNonNull(err, "err must not be null");
    }

    public void handle(Exception exception) {
        Objects.requireNonNull(exception, "exception must not be null");

        if (exception instanceof TaskPulseException) {
            err.println("Error: " + exception.getMessage());
        } else if (exception instanceof IllegalArgumentException) {
            err.println("Invalid argument: " + exception.getMessage());
        } else {
            err.println("Unexpected error: " + exception.getMessage());
        }
    }
}
