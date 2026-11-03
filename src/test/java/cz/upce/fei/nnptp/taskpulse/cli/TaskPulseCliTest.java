package cz.upce.fei.nnptp.taskpulse.cli;

import cz.upce.fei.nnptp.taskpulse.domain.Priority;
import cz.upce.fei.nnptp.taskpulse.domain.Task;
import cz.upce.fei.nnptp.taskpulse.domain.TaskState;
import cz.upce.fei.nnptp.taskpulse.service.ProjectService;
import cz.upce.fei.nnptp.taskpulse.service.TaskService;
import cz.upce.fei.nnptp.taskpulse.storage.InMemoryProjectRepository;
import cz.upce.fei.nnptp.taskpulse.storage.InMemoryTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

class TaskPulseCliTest {

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private ProjectService projectService;
    private TaskService taskService;
    private TaskPulseCli cli;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8);

        InMemoryProjectRepository projectRepo = new InMemoryProjectRepository();
        InMemoryTaskRepository taskRepo = new InMemoryTaskRepository();
        projectService = new ProjectService(projectRepo, taskRepo);
        taskService = new TaskService(taskRepo, projectRepo);

        cli = new TaskPulseCli(projectService, taskService, out, err);
    }

    @Test
    void helpCommandDisplaysAvailableCommands() {
        boolean continueRunning = cli.executeLine("help");
        assertThat(continueRunning).isTrue();
        assertThat(outContent.toString()).contains("Available commands:", "project add", "task add", "worklog log",
                "--exceeded [<minutes>]");
    }

    @Test
    void exitCommandReturnsFalse() {
        boolean continueRunning = cli.executeLine("exit");
        assertThat(continueRunning).isFalse();

        continueRunning = cli.executeLine("quit");
        assertThat(continueRunning).isFalse();
    }

    @Test
    void projectLifecycleViaCli() {
        cli.executeLine("project add \"Programming Course\" \"Semester project\"");
        assertThat(outContent.toString()).contains("Project created:", "Programming Course");

        outContent.reset();
        cli.executeLine("project list");
        assertThat(outContent.toString()).contains("Programming Course", "Semester project");
    }

    @Test
    void taskAndWorklogLifecycleViaCli() {
        var project = projectService.createProject("Course", "Desc");
        String projectId = project.getId().toString();

        cli.executeLine("task add " + projectId + " \"Implement parser\" \"RFC 4180\"");
        assertThat(outContent.toString()).contains("Task created:", "Implement parser");

        var task = taskService.listTasks().get(0);
        String taskId = task.getId().toString();

        cli.executeLine("task transition " + taskId + " IN_PROGRESS");
        assertThat(outContent.toString()).contains("transitioned to IN_PROGRESS");

        cli.executeLine("worklog log " + taskId + " 45 --author \"Student\" --desc \"First pass\"");
        assertThat(outContent.toString()).contains("Work logged: 45 min by Student");

        cli.executeLine("task transition " + taskId + " DONE");
        assertThat(outContent.toString()).contains("transitioned to DONE");
    }

    @Test
    void unknownCommandDisplaysClearErrorMessage() {
        cli.executeLine("unknownCommand 123");
        assertThat(errContent.toString()).contains("Unknown command: 'unknowncommand'");
    }

    @Test
    void invalidUuidDisplaysClearErrorMessageWithoutStackTrace() {
        cli.executeLine("project show not-a-uuid");
        assertThat(errContent.toString()).contains("Invalid UUID format: 'not-a-uuid'");
        assertThat(errContent.toString()).doesNotContain("Exception in thread", "at cz.upce");
    }

    @Test
    void interactiveSessionCleanExitOnEof() {
        Scanner scanner = new Scanner("help\nexit\n");
        cli.runInteractive(scanner);
        assertThat(outContent.toString()).contains("TaskPulse CLI", "Available commands:");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "60, 60 min, -15 min",
            "0, 0 min, 45 min",
            "none, not estimated, unavailable"
    }, nullValues = "none")
    void taskShowReportsEstimateActualAndVariance(Integer estimate, String expectedEstimate, String expectedVariance) {
        Task task = taskWith45RecordedMinutes(estimate);
        cli.executeLine("task show " + task.getId());
        assertThat(outContent.toString()).contains("Estimate:    " + expectedEstimate, "2 (45 min recorded)",
                "Variance:    " + expectedVariance);
    }

    @ParameterizedTest
    @CsvSource({
            "'', Review",
            "--exceeded --priority HIGH, Review | 15 min over estimate",
            "--exceeded 14, Review | 15 min over estimate",
            "--exceeded 15, No tasks found.",
            "--priority LOW --exceeded, No tasks found."
    })
    void taskListShowsOnlyMatchingVarianceAlerts(String options, String expectedOutput) {
        taskWith45RecordedMinutes(30);
        cli.executeLine("task list " + options);
        assertThat(outContent.toString().stripTrailing()).endsWith(expectedOutput);
        assertThat(errContent.toString()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"-1, non-negative", "later, whole number", "9223372036854775808, whole number"})
    void taskListRejectsInvalidVarianceThreshold(String input, String expectedError) {
        cli.executeLine("task list --exceeded " + input);
        assertThat(errContent.toString()).contains(expectedError);
        assertThat(outContent.toString()).isEmpty();
    }

    private Task taskWith45RecordedMinutes(Integer estimate) {
        var project = projectService.createProject("Planning", "");
        Task task = taskService.createTask(project.getId(), "Review", "", Priority.HIGH, estimate, null);
        taskService.transitionTask(task.getId(), TaskState.IN_PROGRESS);
        taskService.logWork(task.getId(), 20, "Dev", "");
        taskService.logWork(task.getId(), 25, "Dev", "");
        return task;
    }
}
