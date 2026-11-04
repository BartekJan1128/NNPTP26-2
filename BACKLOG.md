# TaskPulse Backlog

This backlog categorizes candidate student tasks across defects, refactoring, new features, and tests.

## Defects (Bug Fixes)
- **BUG-01**: Rejection of empty label list edge cases when deserializing CSV.
- **BUG-02**: Enforce strict validation on task estimate changes preventing negative values across all update paths.
- **BUG-03**: Handle edge case in CSV parsing when records end with trailing empty lines with actionable exception message.
- **BUG-04**: Prevent updating tasks to reference nonexistent project IDs if a project update endpoint/method is added.

## Refactoring & Design Improvements
- **REF-01**: Decompose `TaskPulseCli` by introducing dedicated command handler classes via Command pattern.
- **REF-02**: Extract state machine transitions into State design pattern or discrete transition handlers.
- **REF-03**: Encapsulate Task creation with a dedicated TaskBuilder to simplify test and persistence construction.
- **REF-04**: Decouple CSV serialization logic into dedicated entity mappers (`ProjectCsvMapper`, `TaskCsvMapper`, `WorklogCsvMapper`).

## New Features
- **FEAT-01**: Implement FR-08 Markdown project summary exporter.
- **FEAT-02**: Implement FR-08 JSON project summary exporter.
- **FEAT-03**: Support `--format` flag in CLI report commands (`report export <project-id> --format <markdown|json>`).

## Tests
- **TEST-01**: Add property-based / randomized tests for CSV escaping and round-trip preservation.
- **TEST-02**: Add integration tests verifying end-to-end CLI workflow via stdin/stdout piping.
- **TEST-03**: Increase branch coverage for edge cases in date range queries and time parsing.
