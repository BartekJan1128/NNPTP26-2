package cz.upce.fei.nnptp.taskpulse.domain;

import cz.upce.fei.nnptp.taskpulse.domain.exception.TaskTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskLifecycleTest {

    @ParameterizedTest(name = "{0} -> {1}, worklog={2}, allowed={3}")
    @MethodSource("transitionCases")
    void allTransitionsPreserveExistingRules(TaskState source, TaskState target,
                                            boolean hasWorklog, boolean allowed) {
        Worklog worklog = new Worklog(LocalDateTime.of(2026, 10, 21, 10, 0), 30, "Tester");
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), "Title", "Description",
                Priority.HIGH, source, 60, LocalDate.of(2026, 12, 1), List.of("backend"),
                hasWorklog ? List.of(worklog) : List.of());

        if (allowed) {
            task.transitionTo(target);
            assertThat(task.getState()).isEqualTo(target);
        } else {
            String message = source == target ? "Task is already in state " + source
                    : source == TaskState.IN_PROGRESS && target == TaskState.DONE && !hasWorklog
                    ? "Cannot complete task in IN_PROGRESS state without any worklogs"
                    : "Invalid state transition from " + source + " to " + target;
            assertThatThrownBy(() -> task.transitionTo(target))
                    .isInstanceOf(TaskTransitionException.class)
                    .hasMessage(message);
            assertThat(task.getState()).isEqualTo(source);
        }

        assertThat(task.getTitle()).isEqualTo("Title");
        assertThat(task.getDescription()).isEqualTo("Description");
        assertThat(task.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(task.getEstimatedMinutes()).isEqualTo(60);
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(task.getLabels()).containsExactly("backend");
        assertThat(task.getWorklogs()).isEqualTo(hasWorklog ? List.of(worklog) : List.of());
    }

    private static Stream<Arguments> transitionCases() {
        return Stream.of(TaskState.values()).flatMap(source ->
                Stream.of(TaskState.values()).flatMap(target ->
                        Stream.of(false, true).map(hasWorklog -> {
                            boolean allowed = switch (source) {
                                case TODO, BLOCKED -> target == TaskState.IN_PROGRESS;
                                case IN_PROGRESS -> target == TaskState.BLOCKED
                                        || (target == TaskState.DONE && hasWorklog);
                                case DONE -> target == TaskState.TODO;
                            };
                            return Arguments.of(source, target, hasWorklog, allowed);
                        })));
    }

    @ParameterizedTest
    @EnumSource(TaskState.class)
    void nullTargetIsRejectedWithoutChangingState(TaskState source) {
        Task task = new Task(UUID.randomUUID(), UUID.randomUUID(), "Title", "", Priority.MEDIUM,
                source, null, null, null, null);

        assertThatThrownBy(() -> task.transitionTo(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Next state must not be null");
        assertThat(task.getState()).isEqualTo(source);
    }

    @Test
    void standardLifecycleFlow() {
        UUID projectId = UUID.randomUUID();
        Task task = new Task(projectId, "Feature X");

        assertThat(task.getState()).isEqualTo(TaskState.TODO);

        task.transitionTo(TaskState.IN_PROGRESS);
        assertThat(task.getState()).isEqualTo(TaskState.IN_PROGRESS);

        task.transitionTo(TaskState.BLOCKED);
        assertThat(task.getState()).isEqualTo(TaskState.BLOCKED);

        task.transitionTo(TaskState.IN_PROGRESS);
        assertThat(task.getState()).isEqualTo(TaskState.IN_PROGRESS);

        task.addWorklog(new Worklog(LocalDateTime.now(), 50, "Dev", "Feature work"));
        task.transitionTo(TaskState.DONE);
        assertThat(task.getState()).isEqualTo(TaskState.DONE);

        task.transitionTo(TaskState.TODO);
        assertThat(task.getState()).isEqualTo(TaskState.TODO);
        assertThat(task.getWorklogs()).hasSize(1);
    }

    @Test
    void transitionFromInProgressToDoneWithoutWorklogsIsRejected() {
        UUID projectId = UUID.randomUUID();
        Task task = new Task(projectId, "Feature Y");
        task.transitionTo(TaskState.IN_PROGRESS);

        assertThatThrownBy(() -> task.transitionTo(TaskState.DONE))
                .isInstanceOf(TaskTransitionException.class)
                .hasMessageContaining("Cannot complete task in IN_PROGRESS state without any worklogs");

        assertThat(task.getState()).isEqualTo(TaskState.IN_PROGRESS);
    }

    @ParameterizedTest
    @EnumSource(TaskState.class)
    void transitionToSameStateIsRejected(TaskState state) {
        UUID projectId = UUID.randomUUID();
        Task task = new Task(UUID.randomUUID(), projectId, "Title", "", Priority.MEDIUM, state, null, null, null, null);

        assertThatThrownBy(() -> task.transitionTo(state))
                .isInstanceOf(TaskTransitionException.class)
                .hasMessageContaining("already in state");
    }

    @Test
    void invalidTransitionsAreRejectedAndDoNotMutateTask() {
        UUID projectId = UUID.randomUUID();
        Task task = new Task(projectId, "Feature Z");

        assertThatThrownBy(() -> task.transitionTo(TaskState.DONE))
                .isInstanceOf(TaskTransitionException.class);
        assertThat(task.getState()).isEqualTo(TaskState.TODO);

        assertThatThrownBy(() -> task.transitionTo(TaskState.BLOCKED))
                .isInstanceOf(TaskTransitionException.class);
        assertThat(task.getState()).isEqualTo(TaskState.TODO);

        task.transitionTo(TaskState.IN_PROGRESS);
        task.transitionTo(TaskState.BLOCKED);

        assertThatThrownBy(() -> task.transitionTo(TaskState.DONE))
                .isInstanceOf(TaskTransitionException.class);
        assertThat(task.getState()).isEqualTo(TaskState.BLOCKED);

        task.transitionTo(TaskState.IN_PROGRESS);
        task.addWorklog(new Worklog(LocalDateTime.now(), 20, "Author"));
        task.transitionTo(TaskState.DONE);

        assertThatThrownBy(() -> task.transitionTo(TaskState.IN_PROGRESS))
                .isInstanceOf(TaskTransitionException.class);
        assertThat(task.getState()).isEqualTo(TaskState.DONE);

        assertThatThrownBy(() -> task.transitionTo(TaskState.BLOCKED))
                .isInstanceOf(TaskTransitionException.class);
        assertThat(task.getState()).isEqualTo(TaskState.DONE);
    }

    @Test
    void reopeningPreservesAllTaskAttributes() {
        UUID projectId = UUID.randomUUID();
        Task task = new Task(projectId, "Preserved Task");
        task.setEstimatedMinutes(120);
        task.setDueDate(LocalDate.of(2026, 12, 1));
        task.addLabel("critical");
        task.transitionTo(TaskState.IN_PROGRESS);
        task.addWorklog(new Worklog(LocalDateTime.now(), 60, "Tester"));
        task.transitionTo(TaskState.DONE);

        task.transitionTo(TaskState.TODO);

        assertThat(task.getState()).isEqualTo(TaskState.TODO);
        assertThat(task.getEstimatedMinutes()).isEqualTo(120);
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(task.getLabels()).containsExactly("critical");
        assertThat(task.getWorklogs()).hasSize(1);
    }
}
