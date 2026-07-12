<!-- Keep a Changelog guide: https://keepachangelog.com -->

# YCPCS Marmoset Submitter Changelog

## [Unreleased]

### Added
- Run Configuration based submission mode for multi-assignment CLion projects.
  When enabled via `useRunConfigurationBasedSubmissions=true`, the plugin reads
  the currently selected JetBrains Run Configuration name and resolves the
  corresponding assignment info file via a new assignment info mapping file
  (`assignment_info_mapping.cmake`). Only the files in the directory containing
  the resolved assignment info file are zipped and submitted.
- Support for both subdirectory style and project root style assignment info
  mapping files, allowing flexible project structures.

### Changed
- `assignmentInfoFilename` property now serves dual purpose. In Mode 1
  (single assignment) it specifies the assignment info file directly; in Mode 2
  (multi-assignment) it specifies the assignment info mapping file.
- In Mode 2, zip files are created inside the assignment subdirectory rather
  than the project root.

## [1.2.0] - 2026-05-19

### Added

- Added support wildcards in `excludedFilenames` and `excludedDirectories` properties

## [1.1.0] - 2026-03-26

### Added

- Added support for IntelliJ Platform versions dating back to 2024.2 (build 242)

## [1.0.0] - 2026-03-18

### Added

- Initial release
- Submit projects to Marmoset directly from any JetBrains IDE
- Configurable file inclusion and exclusion rules via marmoset_submitter.properties
- Secure credential storage using the IntelliJ Platform PasswordSafe API
- Progress dialog with cancellation support during zip file creation

[Unreleased]: https://github.com/jmoscola/ycpcs_marmoset_submitter/compare/1.2.0...HEAD
[1.2.0]: https://github.com/jmoscola/ycpcs_marmoset_submitter/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/jmoscola/ycpcs_marmoset_submitter/compare/1.0.0...1.1.0
[1.0.0]: https://github.com/jmoscola/ycpcs_marmoset_submitter/commits/1.0.0
