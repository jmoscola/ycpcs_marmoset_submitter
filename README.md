# ![icon](src/main/resources/META-INF/pluginIcon.svg) YCPCS Marmoset Submitter

<!-- Plugin description -->
An IntelliJ Platform plugin that automates the submission of student programming
assignments to the [YCPCS Marmoset](https://cs.ycp.edu/marmoset) submission server.
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
  - [CMake Assignment Info File](#cmake-assignment-info-file)
  - [Plugin Configuration File](#plugin-configuration-file)
- [Usage](#usage)
- [For Developers](#for-developers)
- [License](#license)

---

## Requirements

- IntelliJ-based IDE (IntelliJ IDEA, CLion, PyCharm, WebStorm, etc.)
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

### Instructor Setup

Each project that uses this plugin must contain two configuration files in its
root directory:

| File | Purpose |
|------|---------|
| `CMakeLists.assignment_info.txt` | Identifies the course, term, and assignment |
| `marmoset_submitter.properties` | Configures the plugin's submission behavior |

A template for `marmoset_submitter.properties` is provided in the `templates/`
directory of this repository. Copy it to your project root and fill in the
appropriate values.

---

### CMake Assignment Info File

Create a file named `CMakeLists.assignment_info.txt` in the root directory of
the student's project with the following format:
```cmake
set(COURSE_NAME "CS 350")
set(TERM "Fall")
set(YEAR "2026")  # optional, include when useAssignmentInfoYear=true in marmoset_submitter.properties
string(TIMESTAMP CURRENT_YEAR "%Y")
set(SEMESTER "${TERM} ${CURRENT_YEAR}")
set(PROJECT_NUMBER "assign01")
set(PROJECT_NAME_STR IntArrayStack)
```

| Field | Description | Must Match Marmoset? |
|-------|-------------|----------------------|
| `COURSE_NAME` | The name of the course | Yes |
| `TERM` | The academic term (`Fall`, `Spring`, or `Summer`) | Yes |
| `YEAR` | The submission year. Only required when `useAssignmentInfoYear=true` in `marmoset_submitter.properties`. When omitted or when `useAssignmentInfoYear=false`, the current system year is used automatically. | No |
| `PROJECT_NUMBER` | The assignment identifier | Yes |
| `SEMESTER` | Derived from `TERM` and `CURRENT_YEAR` (used by CMake, not the plugin) | — |
| `PROJECT_NAME_STR` | The project name (used by CMake, not the plugin) | No |

---

### Plugin Configuration File

Create a file named `marmoset_submitter.properties` in the root directory of
the student's project. A fully commented template is available in `templates/marmoset_submitter.properties.example`.

#### Required Properties

| Property | Description |
|----------|-------------|
| `submissionUrl` | The URL of the Marmoset submission server |
| `assignmentInfoFilename` | The name of the CMake assignment info file |

#### Optional Properties

| Property | Description | Default |
|----------|-------------|---------|
| `allowedFilenames` | Comma-separated whitelist of exact filenames to include in the submission zip file (e.g. `main.cpp,main.h,Makefile`). When set to one or more values, only files whose names appear in this list will be included, regardless of their extension. This is a more restrictive filter than `allowedExtensions`, when both are set, a file must satisfy both filters to be included. When omitted entirely from the properties file, all filenames are considered for inclusion, subject to the other rules. **Important:** if this property is present in the file but left empty (e.g. `allowedFilenames=`), it is treated as an empty set and no files will be included in the zip file, resulting in an empty submission. | All filenames allowed |
| `allowedExtensions` | Comma-separated whitelist of file extensions to include in the submission zip file, without leading dots (e.g. `h,cpp`). When set to one or more values, only files whose extensions appear in this list will be included. Files with any other extension will be excluded regardless of their location in the project. When omitted entirely from the properties file, files of all extensions are considered for inclusion, subject to the other exclusion rules. **Important:** if this property is present in the file but left empty (e.g. `allowedExtensions=`), it is treated as an empty set and no files will be included in the zip file, resulting in an empty submission.                                      | All extensions allowed |
| `excludedFilenames` | Comma-separated list of exact filenames to unconditionally exclude from the submission zip file (e.g. `.DS_Store,Flags.h,tests.cpp`). Any file whose name appears in this list will be excluded regardless of its location in the project directory tree. Useful for excluding instructor-provided files that students should not modify or submit.                                                                                                                                                                                                                                                                                                                                                                        | None |
| `excludedDirectories` | Comma-separated list of directory names to exclude from the submission zip file (e.g. `.git,.idea,build,out`). When a directory name appears in this list, the directory will not be recursed. Neither the directory itself nor any of its contents will be included in the submission, regardless of the other inclusion rules.                                                                                                                                                                                                                                                                                                                                                                                           | None |
| `excludedExtensions` | Comma-separated list of file extensions to unconditionally exclude from the submission zip file, without leading dots (e.g. `o,d,a,log,exe,zip`). Any file whose extension appears in this list will be excluded regardless of its location in the project directory tree. Useful for excluding build artifacts and other generated files that should not be submitted.                                                                                                                                                                                                                                                                                                                                                    | None |
| `zipFilenameSuffix` | The suffix appended to the project number to form the zip filename. For example, a project number of `assign01` and a suffix of `_submission` produces a zip file named `assign01_submission.zip`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | `_submission` |
| `useAssignmentInfoYear` | Specifies whether the submission year should be read from the `YEAR` field in the CMake assignment info file (`true`) or determined automatically from the current system year via `java.time.Year.now()` (`false`). When set to `true`, the `YEAR` field must be present in the CMake assignment info file. **Note:** only the exact value `true` (case-insensitive) is treated as true — any other value including `yes` or `1` defaults to `false`. | `false` |

#### Example

```properties
submissionUrl=https://cs.ycp.edu/marmoset/bluej/SubmitProjectViaBlueJSubmitter
assignmentInfoFilename=CMakeLists.assignment_info.txt
allowedFilenames=main.cpp,main.h,Makefile
allowedExtensions=h,cpp
excludedFilenames=.DS_Store,Flags.h,tests.cpp
excludedDirectories=.git,.idea,.vs,.gradle,build,out,target,node_modules,cmake-build-debug
excludedExtensions=o,d,a,iml,log,stackdump,exe,zip
zipFilenameSuffix=_submission
useAssignmentInfoYear=false
```

---

## Usage

Once the plugin is installed and the project is configured:

1. Open the student's project in your JetBrains IDE.
2. Click the **Marmoset Submitter** button (![icon](src/main/resources/icons/MarmosetSubmit.svg)) in the main toolbar, or go to **Tools → Submit to Marmoset**.
3. The plugin will scan and zip the project files according to the configured rules.
4. Enter your Marmoset **username** and **password** in the login dialog. Previously saved credentials will be pre-populated automatically.
5. The plugin will upload the zip file to the Marmoset server and display a confirmation dialog on success.

### Error Messages

| Error | Cause |
|-------|-------|
| `Missing required property '...'` | A required property is absent from `marmoset_submitter.properties` |
| `Assignment info file not found` | The file specified by `assignmentInfoFilename` does not exist |
| `Invalid username or password` | The credentials provided were rejected by the Marmoset server (HTTP 403) |
| `Invalid course or assignment name` | The course name or assignment name does not match Marmoset (HTTP 404) |

---

## For Developers

### Project Structure

```
ycpcs-marmoset-submitter/
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── edu/ycp/cs/marmosetsubmitter/
│       │       ├── actions/         # AnAction implementations
│       │       ├── dialog/          # DialogWrapper implementations
│       │       └── services/        # Business logic and data classes
│       └── resources/
│           ├── icons/               # Plugin and action icons
│           ├── messages/            # Localization resource bundles
│           └── META-INF/            # Plugin configuration (plugin.xml)
├── templates/
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

Set the above values as environment variables.  For example, on macOS, set these 
values in the `~/.zshrc` file.

Alternatively, you can set `clionPath` in a `local.properties` file in the
project root instead of using the environment variable:
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