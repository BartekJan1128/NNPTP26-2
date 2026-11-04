# Proposed Future Code Changes for TaskPulse

This document outlines proposed changes to evolve the TaskPulse codebase across refactoring, design pattern implementations, bug fixes, and feature additions.

---

## 1. Refactoring for Cleaner Code

- **Decompose `TaskPulseCli`**:
  - Split the monolithic switch statement in `TaskPulseCli` into dedicated handler methods or classes per domain aggregate (`ProjectCliHandler`, `TaskCliHandler`, `WorklogCliHandler`).
  - Extract command-line argument parsing and flag handling (`--project`, `--state`, `--priority`, etc.) into a reusable CLI options parser.

- **Separate CSV Row Mapping from Persistence Workflow**:
  - Extract entity-to-CSV row serialization and row-to-entity parsing out of `CsvStorageManager` into dedicated entity mappers:
    - `ProjectCsvMapper`
    - `TaskCsvMapper`
    - `WorklogCsvMapper`
  - Keep `CsvStorageManager` focused solely on file I/O, transactional consistency, and relationship validation.

- **Extract Query Filtering Logic**:
  - Extract query predicate matching logic from `TaskService.matchesQuery(...)` into a dedicated query evaluation component or filter predicates.

- **Standardize Exception Translation**:
  - Unify CLI error presentation through a dedicated error formatting handler rather than multiple catch blocks inside `executeLine`.

---

## 2. Design Pattern Implementations

- **Command Pattern (CLI Layer)**:
  - Define an interface `CliCommand` with methods `String getName()`, `String getUsage()`, and `void execute(List<String> args)`.
  - Implement concrete commands (`ProjectAddCommand`, `TaskListCommand`, `WorklogLogCommand`, etc.) and register them into a `CommandRegistry`.
  - Enables open-closed extensibility for adding new CLI commands without modifying core CLI runners.

- **State Pattern (Domain Layer)**:
  - Replace the enum switch transition rules in `Task` with the State design pattern.
  - Create a `TaskState` interface or abstract base class with state implementations (`TodoState`, `InProgressState`, `BlockedState`, `DoneState`).
  - Each state encapsulates transition permissions and action constraints (such as disallowing worklogs unless in `InProgressState`).

- **Specification Pattern (Query & Filtering)**:
  - Create composable specifications (`TaskSpecification`) with implementations such as `ProjectIdSpecification`, `StateSpecification`, `OverdueSpecification`, and `TextSearchSpecification`.
  - Support chaining with `and()`, `or()`, and `not()`.

- **Strategy Pattern (Report Exporters)**:
  - Define a `ReportExporter` interface.
  - Implement concrete export strategies: `MarkdownReportExporter` and `JsonReportExporter` to satisfy FR-08 requirements.

- **Unit of Work Pattern (Persistence Layer)**:
  - Introduce a `WorkspaceUnitOfWork` to track modified entities and commit atomic writes to all CSV files (`projects.csv`, `tasks.csv`, `worklogs.csv`) with rollback on failure.

---

## 3. Bug Fixes & Edge Case Handling

- **Strict Validation on Task Updates**:
  - Ensure `TaskService.updateTask` validates against negative estimates and blank titles consistently when updating existing tasks.

- **Escaping Special Characters in CLI Arguments**:
  - Enhance `CommandTokenizer` to handle standard escape sequences (`\n`, `\t`, `\\`) inside quoted strings.

- **Case-Insensitive Enum Parsing in CLI**:
  - Normalize user-supplied enum values in CLI commands before validation to provide friendlier messages for case discrepancies.

---

## 4. New Feature Implementations

- **FR-08 Project Summary Reports**:
  - Export project metrics and task breakdown as Markdown tables and JSON documents.
  - Include task counts by state, overdue counts, total recorded time, and estimate variance.

- **Report Export CLI Command**:
  - Add CLI command: `report export <project-id> --format <markdown|json> [--output <file-path>]`.

- **Interactive Task Querying and Sorting Options**:
  - Expose `--sort` flag in `task list` (`--sort title`, `--sort priority`, `--sort duedate`).
  - Add date range filtering options (`--from <date>`, `--to <date>`) and `--overdue` flag to the CLI.

- **Task Estimates vs. Actuals Variance Alerts**:
  - Provide a CLI view or command highlighting tasks that have exceeded their estimated time (`task list --exceeded`).

- **Workspace Backup and Restore**:
  - Add commands to export the entire workspace into a compressed archive or import from an external CSV bundle.
