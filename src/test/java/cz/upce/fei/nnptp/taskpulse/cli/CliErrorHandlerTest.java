package cz.upce.fei.nnptp.taskpulse.cli;

import cz.upce.fei.nnptp.taskpulse.domain.exception.TaskPulseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CliErrorHandlerTest {

    private ByteArrayOutputStream errContent;
    private CliErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        errContent = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8);
        errorHandler = new CliErrorHandler(err);
    }

    @Test
    void handlesTaskPulseException() {
        errorHandler.handle(new TaskPulseException("Task not found"));

        assertThat(errContent.toString(StandardCharsets.UTF_8))
                .isEqualTo("Error: Task not found" + System.lineSeparator());
    }

    @Test
    void handlesIllegalArgumentException() {
        errorHandler.handle(new IllegalArgumentException("Invalid UUID"));

        assertThat(errContent.toString(StandardCharsets.UTF_8))
                .isEqualTo("Invalid argument: Invalid UUID" + System.lineSeparator());
    }

    @Test
    void handlesUnexpectedException() {
        errorHandler.handle(new RuntimeException("Something went wrong"));

        assertThat(errContent.toString(StandardCharsets.UTF_8))
                .isEqualTo("Unexpected error: Something went wrong" + System.lineSeparator());
    }
}