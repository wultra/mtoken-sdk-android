# Welcome to the Wultra Mobile Token SDK Android repository!

In this file, you'll find several topics that will help you with the contribution process, how to run tests, how to create pull requests, and how to prepare a new release.

## Table of Contents
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Running Tests](#running-tests)
- [Creating a Pull Request](#creating-a-pull-request)
- [Preparing a New Release](#preparing-a-new-release)

> [!NOTE]
> Comapny-wide policies and guidelines for contributing can be found in the [Wultra Contributing Guidelines](https://github.com/wultra/.github/blob/main/CONTRIBUTING.md).
## Getting Started

> [!WARNING]
> If you're not a Wultra employee or contractor, please fill out the [Wultra Contributor License Agreement](https://forms.gle/r715RoVDoji4GD7K7) before you start contributing.

Before you start development, make sure you have the following prerequisites:
- macOS machine preferred (build scripts etc are not tested on Linux or Windows)
- [Android Studio](https://developer.android.com/studio) installed
- [Java JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) installed

Now you should be able to open the project in Android Studio and start developing, or try to build the project from the command line:

```bash
./gradlew clean build
```

## Project Structure

The project structure is organized as follows (the most important files and directories):

```
mtoken-sdk-android/
├── .github/                            # GitHub-related files (workflows, contributing guidelines)
|── buildSrc/                           # Gradle build scripts
├── configs/                            # Configuration files for tests
├── docs/                               # Documentation files that will be published on developers.wultra.com
├── library/                            # Main folder with the project source code and gradle build scripts
├── README.md                           # Project overview and documentation
|── scripts/                            # Scripts for building, testing, releasing
├── .editorconfig                       # Editor configuration file for coding standards
└── .prepare-release.json               # Configuration for the prepare-release script
```

## Running Tests

Before you run the tests, make sure:
- the `configs/integration-tests.properties` or `configs/private-integration-tests.properties` file is set up correctly.
- a Android device is connected to your computer or an emulator is running

```bash
# run integration tests
./gradlew clean connectedAndroidTest --info
```

```bash
# run unit tests
./gradlew clean test
```

## Creating a Pull Request

> [!WARNING]
> Before you create a pull request make sure that: 
> 
> - an issue is created for the change you want to make. If there is no issue, create one first.
> - all tests are passing.
> - `sh scripts/lint.sh` does not report any issues

0. If you're not a Wultra employee or contractor, fork the repository and make changes in your fork. If you're a Wultra employee or contractor, you can make changes directly in the repository.
1. Create a new branch for your changes. The branch name should follow the format `issues/issue-number-short-description`, e.g. `issues/123-fix-bug-in-inbox`.
2. Make your changes and commit them with a clear commit message that describes the changes you made.
3. Push your changes to the remote repository.
4. Create a pull request from your branch to the `develop` branch of the repository.
    - Pick a descriptive title for your pull request that summarizes the changes you made.
    -  In the pull request description, reference the issue you are addressing by using `#issue-number`, e.g. `#123`. This will automatically link the pull request to the issue.
    - If you're not a Wultra employee or contractor, wait for a Wultra team member to review your pull request and approve workflows to run.
    - If you're a Wultra employee or contractor, wait for all workflows to pass and then request review from a Wultra maintainer.

## Preparing a New Release

> [!WARNING]
> This section is intended for Wultra employees and contractors only. If you are not a Wultra employee or contractor, please do not attempt to prepare a new release.

### Some important notes regarding the release streams (branches)

- the `develop` branch is used for development and should not be used for releases
- each release stream should be created from the `develop` branch (can be specific commit in the history)
- naming of the release stream should follow the format `releases/a.b.x`, e.g. `releases/1.0.x`
- git history of the release stream should be always linear, i.e. no merge commits should be present in the history
- every change into the release stream should be done via a pull request that will be squashed and merged into the release stream

### Release Versioning

The version number is composed of three parts: `major.minor.patch`, e.g. `1.0.0`.

- The `major` version is incremented when a milestone is reached, e.g. a platform version is updated or major principle is changed.
- The `minor` version is incremented when a new feature is added or a significant change is made. Minor changes can be API incompatible, but they should not break existing functionality.
- The `patch` version is incremented when a bug fix is introduced. No API changes are allowed in patch releases, and they should not break existing functionality or introduce new features.

### Each release should contain following changes

> [!TIP]
> You can use the `scripts/prepare-release.sh` script to prepare all the necessary files for a new release.
> This script will use `.prepare-release.json` file to determine which files should be updated.
> 
> If you pass a `--verify` flag to the script, it will check if all the files are updated correctly and will not allow you to proceed with the release if any of the files are not updated.

- updated `library/gradle.properties` file with the new version number
- updated `docs/Changelog.md` file with the new version number and a summary of the changes for Wultra developers documentation
- updated `docs/SDK-Integration.md` file with the new version number for the integration example
- _(if needed)_ updated `docs/SDK-Integration.md` file with compatibility information

### Creating a Release (example scenario)

> [!NOTE]
> This scenario describes how to create a new `1.2.0` release of the SDK from the HEAD of the `develop` branch.

1. Create an issue for the new release, e.g. `Prepare release 1.2.0`. Add info what is the reason for the release.
2. Prepare new branch from `develop` branch without any changes (if a release branch does not exist yet) and push it to the remote repository without any changes. Release branches are protected and can be created only by Wultra employees or contractors.
3. Create a new branch for the exact release (for example `issues/65-prepare-release-1_2_0`).
4. Make sure that all the files mentioned in the "[each release should contain following changes](#each-release-should-contain-following-changes)" section are updated correctly.
5. Commit the changes with a clear commit message, e.g. `Prepare release 1.2.0`.
6. Push the changes to the remote repository.
7. Create a pull request from the `issues/65-prepare-release-1_2_0` branch to the `release/1.2.x` branch.
    - The pull request title should be `Prepare release 1.2.0`.
    - The pull request description should reference the issue you created in the first step, e.g. `#65`.
8. Wait for the pull request to be reviewed and approved by a Wultra team member.
9. Once the pull request is approved, merge it into the `releases/1.2.x` branch using the "Squash and merge" option. This will ensure that the git history is linear, and the commit message is clear.
10. Go to the [Wultra Azure DevOps portal](https://dev.azure.com/wultra) and run the release pipeline with required parameters.
12. After the release pipeline is finished, you have to go to the `https://central.sonatype.com/`, sign in with the Wultra account, and release the artifacts that were published to the staging repository.
11. Create a new release on GitHub:
    - Go to the "Releases" section of the repository.
    - Click on "Draft a new release".
    - Select the `1.2.0` tag you just created.
    - Fill in the release title and description. The description should contain a summary of the changes made in the release, which can be copied from the `docs/Changelog.md` file.
12. Update the documentation on the [developers.wultra.com](https://developers.wultra.com) portal with the new version information.
