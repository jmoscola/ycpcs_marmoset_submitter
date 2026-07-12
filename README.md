# ![icon](src/main/resources/META-INF/pluginIcon.svg) YCPCS Marmoset Submitter

<!-- Plugin description -->
An IntelliJ Platform plugin that automates the submission of student programming
assignments to a Marmoset submission server.
The plugin zips the project files according to configurable inclusion and exclusion
rules, prompts the student for their Marmoset credentials, and uploads the submission
directly from within the IDE.
<!-- Plugin description end -->

---

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
  - [Instructor Setup](#instructor-setup)
  - [Assignment Info File](#assignment-info-file)
  - [Assignment Info Mapping File](#assignment-info-mapping-file)
  - [Plugin Configuration File](#plugin-configuration-file)
- [Usage](#usage)
  - [Mode 1 -- Single Assignment Project](#mode-1--single-assignment-project)
  - [Mode 2 -- Multi-Assignment Project](#mode-2--multi-assignment-project)
- [For Developers](#for-developers)
- [License](#license)

---

## Requirements

- IntelliJ Platform IDE (IntelliJ IDEA, CLion, PyCharm, WebStorm, etc.)
- JDK 21 or later

---

## Installation

### From the JetBrains Marketplace

1. Open your JetBrains IDE.
2. Go to **Settings → Plugins → Marketplace**.
3. Search for **YCPCS Marmoset Submitter**.
4. Click **Install** and restart the IDE when prompted.

### From a Local Build

1. Clone the repository:
   ```
   git clone https://github.com/jmoscola/ycpcs-marmoset-submitter.git
   ```
2. Build the plugin:
   ```
   ./gradlew buildPlugin
   ```
3. In your JetBrains IDE, go to **Settings → Plugins → ⚙️ → Install Plugin from Disk**.
4. Select the `.zip` file generated in `build/distributions/`.
5. Restart the IDE when prompted.

---

## Configuration

The plugin supports two submission modes, both configured via a single
`marmoset_submitter.properties` file in the project root directory:

| Mode                                    | Description |
|-----------------------------------------|-------------|
| **Mode 1** -- Single assignment project | A single `marmoset_submitter.properties` file and a single assignment info file in the project root. The entire project is zipped and submitted. |
| **Mode 2** -- Multi-assignment project  | A single `marmoset_submitter.properties` file and an assignment info mapping file that maps JetBrains Run Configuration names to individual assignment info files. Only the files in the directory containing the resolved assignment info file are zipped and submitted. |

### Instructor Setup

Each project that uses this plugin must contain the following files in its
root directory:

**Mode 1 -- Single assignment project:**

| File | Purpose |
|------|---------|
| `assignment_info.cmake` | Identifies the course, term, and assignment |
| `marmoset_submitter.properties` | Configures the plugin's submission behavior |

#### File Naming Conventions in Mode 1

> **Important:** The only filename that must match exactly is
> `marmoset_submitter.properties`. The assignment info file may use any
> filename as long as it matches the value specified by
> `assignmentInfoFilename` in `marmoset_submitter.properties`. The
> filename `assignment_info.cmake` is the recommended convention.

For example, both of the following are valid:

```properties
# Using the recommended convention
assignmentInfoFilename=assignment_info.cmake

# Using a custom filename
assignmentInfoFilename=CS350_assign01_info.cmake
```

**Mode 2 -- Multi-assignment project:**

| File | Purpose |
|------|---------|
| `assignment_info_mapping.cmake` | Maps Run Configuration names to assignment info file paths |
| `assignment_info.cmake` (one per assignment) | Identifies the course, term, and assignment for each submission destination |
| `marmoset_submitter.properties` | Configures the plugin's submission behavior |

A fully commented template for `marmoset_submitter.properties` is provided in
the `templates/` directory of this repository. Copy it to your project root
and fill in the appropriate values.

#### File Naming Conventions in Mode 2

> **Important:** The only filename that must match exactly is
> `marmoset_submitter.properties`. All other filenames described in this
> documentation — including `assignment_info.cmake`,
> `assignment_info_mapping.cmake`, and the milestone variants described
> below — are conventions only. Any filename may be used as long as it
> matches the value specified in the relevant configuration file.

For projects that contain multiple milestones within the same assignment,
the recommended convention is to suffix the assignment info filename with
`_ms#` where `#` is the milestone number. Multiple milestone assignment
info files may coexist in the same assignment subdirectory:

---

### Assignment Info File

The assignment info file uses CMake `set()` syntax to define the course name,
term, and project number required for submission. The file may be located in
the project root directory (Mode 1) or in a subdirectory of the project root
(Mode 2), as specified by the assignment info mapping file.

Both quoted and unquoted CMake values are supported.

```cmake
set(COURSE_NAME    "CS 350")
set(TERM           "Fall")
set(YEAR           "2026")
set(PROJECT_NUMBER "assign01")
set(PROJECT_NAME   IntArrayStack)
```

| Field | Description | Must Match Marmoset? |
|-------|-------------|----------------------|
| `COURSE_NAME` | The name of the course | Yes |
| `TERM` | The academic term (`Fall`, `Spring`, or `Summer`) | Yes |
| `YEAR` | The submission year. Only required when `useAssignmentInfoYear=true` in `marmoset_submitter.properties`. When omitted or when `useAssignmentInfoYear=false`, the current system year is used automatically. | No |
| `PROJECT_NUMBER` | The assignment identifier | Yes |
| `PROJECT_NAME` | The project name (used by CMake, not the plugin) | No |

> **Note:** The assignment info file may use any filename as long as it matches
> the value specified by `assignmentInfoFilename` in `marmoset_submitter.properties`
> (Mode 1) or the value specified in the assignment info mapping file (Mode 2).
> The filename `assignment_info.cmake` is the recommended convention.

---

### Assignment Info Mapping File

The assignment info mapping file is used in Mode 2 only. It maps JetBrains Run
Configuration names to their corresponding assignment info file paths using CMake
`set()` syntax. The assignment info files referenced in the mapping may be located
in subdirectories of the project root or in the project root directory itself.

**Subdirectory style** -- one subdirectory per assignment, some with multiple milestones (i.e. _ms#):
```cmake
############################################################
# Mappings from:
# Run Configuration names -> Assignment info files
############################################################
set(DonQuixote      "CS370_Assign01_Fa25/assignment_info.cmake")
set(RollinTrain_MS1 "CS370_Assign02_Fa25/assignment_info_ms1.cmake")
set(RollinTrain_MS2 "CS370_Assign02_Fa25/assignment_info_ms2.cmake")
set(LimeLight_MS1   "CS370_Assign03_Fa25/assignment_info_ms1.cmake")
set(LimeLight_MS2   "CS370_Assign03_Fa25/assignment_info_ms2.cmake")
set(LimeLight_MS3   "CS370_Assign03_Fa25/assignment_info_ms3.cmake")
```

**Project root style** -- all assignment info files in the project root, some with multiple milestones:
```cmake
set(DonQuixote      "assign01_info.cmake")
set(RollinTrain_MS1 "assign02_info_ms1.cmake")
set(RollinTrain_MS2 "assign02_info_ms2.cmake")
set(LimeLight_MS1   "assignment_info_ms1.cmake")
set(LimeLight_MS2   "assignment_info_ms2.cmake")
set(LimeLight_MS3   "assignment_info_ms3.cmake")
```

| Field | Description |
|-------|-------------|
| Key (e.g. `DonQuixote`) | The JetBrains Run Configuration name. Must match exactly, including case. |
| Value (e.g. `"CS370_Assign01_Fa25/assignment_info.cmake"`) | The path to the assignment info file, relative to the project root directory. |

> **Note:** The assignment info mapping file may use any filename as long as it
> matches the value specified by `assignmentInfoFilename` in
> `marmoset_submitter.properties`. The filename `assignment_info_mapping.cmake`
> is the recommended convention.

---

### Plugin Configuration File

Create a file named `marmoset_submitter.properties` in the root directory of
the project. A fully commented template is available in
`templates/marmoset_submitter.properties.example`.

#### Required Properties

| Property | Description |
|----------|-------------|
| `submissionUrl` | The URL of the Marmoset submission server |
| `assignmentInfoFilename` | In Mode 1, the name of the assignment info file. In Mode 2, the name of the assignment info mapping file. |

#### Optional Properties

| Property | Description | Default |
|----------|-------------|---------|
| `useRunConfigurationBasedSubmissions` | Specifies whether the plugin operates in Mode 1 (`false`) or Mode 2 (`true`). In Mode 2, the plugin reads the currently selected JetBrains Run Configuration name, looks it up in the assignment info mapping file, and zips only the files in the directory containing the resolved assignment info file. An error is displayed if no Run Configuration is selected or if the selected Run Configuration name is not found in the mapping file. **Note:** only the exact value `true` (case-insensitive) is treated as true -- any other value including `yes` or `1` defaults to `false`. | `false` |
| `allowedFilenames` | Comma-separated whitelist of exact filenames to include in the submission zip file (e.g. `main.cpp,main.h,Makefile`). When set to one or more values, only files whose names appear in this list will be included, regardless of their extension. This is a more restrictive filter than `allowedExtensions` -- when both are set, a file must satisfy both filters to be included. When omitted entirely from the properties file, all filenames are considered for inclusion, subject to the other rules. **Important:** if this property is present in the file but left empty (e.g. `allowedFilenames=`), it is treated as an empty set and no files will be included in the zip file, resulting in an empty submission. | All filenames allowed |
| `allowedExtensions` | Comma-separated whitelist of file extensions to include in the submission zip file, without leading dots (e.g. `h,cpp`). When set to one or more values, only files whose extensions appear in this list will be included. Files with any other extension will be excluded regardless of their location in the project. When omitted entirely from the properties file, files of all extensions are considered for inclusion, subject to the other exclusion rules. **Important:** if this property is present in the file but left empty (e.g. `allowedExtensions=`), it is treated as an empty set and no files will be included in the zip file, resulting in an empty submission. | All extensions allowed |
| `excludedFilenames` | Comma-separated list of exact filenames or wildcard patterns to unconditionally exclude from the submission zip file. Use `*` as a wildcard to match any sequence of characters (e.g. `*_output.txt` excludes all files ending with `_output.txt`, `filename*.txt` excludes all files starting with `filename` and ending with `.txt`). Any file whose name matches any pattern in this list will be excluded regardless of its location in the project directory tree. | None |
| `excludedDirectories` | Comma-separated list of directory names or wildcard patterns to exclude from the submission zip file. Use `*` as a wildcard to match any sequence of characters (e.g. `cmake-build-*` excludes all directories starting with `cmake-build-`). When a directory name matches any pattern in this list, the directory will not be recursed -- neither the directory itself nor any of its contents will be included in the submission, regardless of the other inclusion rules. | None |
| `excludedExtensions` | Comma-separated list of file extensions to unconditionally exclude from the submission zip file, without leading dots (e.g. `o,d,a,log,exe,zip`). Any file whose extension appears in this list will be excluded regardless of its location in the project directory tree. Useful for excluding build artifacts and other generated files that should not be submitted. | None |
| `zipFilenameSuffix` | The suffix appended to the project number to form the zip filename. For example, a project number of `assign01` and a suffix of `_submission` produces a zip file named `assign01_submission.zip`. | `_submission` |
| `useAssignmentInfoYear` | Specifies whether the submission year should be read from the `YEAR` field in the assignment info file (`true`) or determined automatically from the current system year via `java.time.Year.now()` (`false`). When set to `true`, the `YEAR` field must be present in the assignment info file. **Note:** only the exact value `true` (case-insensitive) is treated as true -- any other value including `yes` or `1` defaults to `false`. | `false` |

#### Example -- Mode 1

```properties
submissionUrl=https://cs.ycp.edu/marmoset/bluej/SubmitProjectViaBlueJSubmitter
assignmentInfoFilename=assignment_info.cmake
useRunConfigurationBasedSubmissions=false
allowedExtensions=h,cpp
excludedFilenames=.DS_Store,Flags.h,tests.cpp,*_output.txt
excludedDirectories=.git,.idea,.vs,.gradle,build,out,target,node_modules,cmake-build-*
excludedExtensions=o,d,a,iml,log,stackdump,exe,zip
zipFilenameSuffix=_submission
useAssignmentInfoYear=false
```

#### Example -- Mode 2

```properties
submissionUrl=https://cs.ycp.edu/marmoset/bluej/SubmitProjectViaBlueJSubmitter
assignmentInfoFilename=assignment_info_mapping.cmake
useRunConfigurationBasedSubmissions=true
excludedDirectories=.git,.idea,.vs,.gradle,build,out,target,node_modules,cmake-build-*
excludedExtensions=o,d,a,iml,log,stackdump,exe,zip
zipFilenameSuffix=_submission
useAssignmentInfoYear=false
```

---

## Usage

### Mode 1 -- Single Assignment Project

Once the plugin is installed and the project is configured:

1. Open the project in your JetBrains IDE.
2. Click the **Marmoset Submitter** button (![icon](src/main/resources/icons/MarmosetSubmit.svg)) in the main toolbar, or go to **Tools → Submit to Marmoset**.
3. The plugin will scan and zip the project files according to the configured rules.
4. Enter your Marmoset **username** and **password** in the login dialog. Previously saved credentials will be pre-populated automatically.
5. The plugin will upload the zip file to the Marmoset server and display a confirmation dialog on success.

### Mode 2 -- Multi-Assignment Project

Once the plugin is installed and the project is configured:

1. Open the project in your JetBrains IDE.
2. Select the Run Configuration corresponding to the assignment you wish to submit.
3. Click the **Marmoset Submitter** button (![icon](src/main/resources/icons/MarmosetSubmit.svg)) in the main toolbar, or go to **Tools → Submit to Marmoset**.
4. The plugin will resolve the assignment info file from the mapping file using the selected Run Configuration name, then scan and zip only the files in the directory containing the resolved assignment info file.
5. Enter your Marmoset **username** and **password** in the login dialog. Previously saved credentials will be pre-populated automatically.
6. The plugin will upload the zip file to the Marmoset server and display a confirmation dialog on success.

### Error Messages

| Error | Cause |
|-------|-------|
| `Configuration file not found '...'` | The required `marmoset_submitter.properties` is missing |
| `Missing required property '...'` | A required property is absent from `marmoset_submitter.properties` |
| `Assignment info file not found` | The file specified by `assignmentInfoFilename` does not exist |
| `Assignment info mapping file not found` | The mapping file specified by `assignmentInfoFilename` does not exist (Mode 2 only) |
| `No Run Configuration is selected` | No JetBrains Run Configuration is selected (Mode 2 only) |
| `Run configuration '...' was not found in the mapping file` | The selected Run Configuration name has no entry in the assignment info mapping file (Mode 2 only) |
| `Invalid username or password` | The credentials provided were rejected by the Marmoset server (HTTP 403) |
| `Invalid course or assignment name` | The course name or assignment name does not match Marmoset (HTTP 404) |

---

## For Developers

### Project Structure

```
ycpcs-marmoset-submitter/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── edu/ycp/cs/marmosetsubmitter/
│   │   │       ├── actions/         # AnAction implementations
│   │   │       ├── dialog/          # DialogWrapper implementations
│   │   │       └── services/        # Business logic and data classes
│   │   └── resources/
│   │       ├── icons/               # Plugin and action icons
│   │       ├── messages/            # Localization resource bundles
│   │       └── META-INF/            # Plugin configuration (plugin.xml)
│   └── test/
│       └── kotlin/
│           └── edu/ycp/cs/marmosetsubmitter/
│               └── services/        # Unit tests
├── templates/
│   ├── assignment_info.cmake.example
│   ├── assignment_info_mapping.cmake.example
│   └── marmoset_submitter.properties.example
├── build.gradle.kts
├── gradle.properties
└── README.md
```

### Building

```
./gradlew buildPlugin
```

### Running

#### In IntelliJ IDEA
```
./gradlew runIde
```

This uses the version of IntelliJ IDEA specified by the `platformVersion`
property in `gradle.properties` and requires no additional configuration.

#### In CLion
```
./gradlew runCLion
```

Requires the `CLION_HOME` environment variable to be set to your local CLion
installation directory:

- **macOS (system-wide install):** `export CLION_HOME=/Applications/CLion.app/Contents`
- **macOS (user-specific install):** `export CLION_HOME=~/Applications/CLion.app/Contents/`
- **Windows:** `set CLION_HOME=C:\Program Files\JetBrains\CLion <version>`
- **Linux:** `export CLION_HOME=/opt/clion-<version>`

Set the above values as environment variables. For example, on macOS, set these
values in the `~/.zshrc` file.

Alternatively, set `clionPath` in a `local.properties` file in the project root
instead of using the environment variable:
```properties
clionPath=/Applications/CLion.app/Contents
```

### Running Tests

```
./gradlew test
```

---

## License

This project is licensed under the MIT License.

```
MIT License

Copyright (c) 2026 jmoscola

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
