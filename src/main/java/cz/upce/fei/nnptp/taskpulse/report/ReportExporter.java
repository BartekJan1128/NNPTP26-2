package cz.upce.fei.nnptp.taskpulse.report;

import cz.upce.fei.nnptp.taskpulse.domain.Project;
import cz.upce.fei.nnptp.taskpulse.domain.Task;

import java.time.LocalDate;
import java.util.List;

/**
 * Exports a project summary report for a given snapshot of tasks.
 * Implementations must not mutate the provided project or tasks.
 */
public interface ReportExporter {
    String export(Project project, List<Task> tasks, LocalDate asOfDate);
}
