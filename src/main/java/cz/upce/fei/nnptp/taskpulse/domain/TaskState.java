package cz.upce.fei.nnptp.taskpulse.domain;

import cz.upce.fei.nnptp.taskpulse.domain.exception.TaskTransitionException;

import java.util.Objects;

public enum TaskState {
    TODO {
        @Override
        void validateDifferentState(TaskState nextState, boolean hasWorklogs) {
            requireTarget(nextState, IN_PROGRESS);
        }
    },
    IN_PROGRESS {
        @Override
        void validateDifferentState(TaskState nextState, boolean hasWorklogs) {
            requireTarget(nextState, BLOCKED, DONE);
            if (nextState == DONE && !hasWorklogs) {
                throw new TaskTransitionException("Cannot complete task in IN_PROGRESS state without any worklogs");
            }
        }
    },
    BLOCKED {
        @Override
        void validateDifferentState(TaskState nextState, boolean hasWorklogs) {
            requireTarget(nextState, IN_PROGRESS);
        }
    },
    DONE {
        @Override
        void validateDifferentState(TaskState nextState, boolean hasWorklogs) {
            requireTarget(nextState, TODO);
        }
    };

    final void validateTransitionTo(TaskState nextState, boolean hasWorklogs) {
        Objects.requireNonNull(nextState, "Next state must not be null");
        if (this == nextState) {
            throw new TaskTransitionException("Task is already in state " + this);
        }
        validateDifferentState(nextState, hasWorklogs);
    }

    abstract void validateDifferentState(TaskState nextState, boolean hasWorklogs);

    final void requireTarget(TaskState nextState, TaskState... allowedStates) {
        for (TaskState allowedState : allowedStates) {
            if (nextState == allowedState) {
                return;
            }
        }
        throw new TaskTransitionException("Invalid state transition from " + this + " to " + nextState);
    }
}
