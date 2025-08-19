# Project Overview

- The SDK is an open-source project
- This project is an SDK for Android that provides a high-level interface for integrating with the PowerAuth authentication system. It is designed to be used in mobile applications to facilitate secure transactions and operations.
- This SDK is built using Kotlin. It leverages the PowerAuth Mobile SDK for underlying cryptographic operations and secure communication.
- This SDK does not provide a user interface but rather focuses on providing a robust API for developers to implement their own UI components.

## Folder Structure

- `/library/src/main`: Contains the source code for the SDK.
- `/library/src/test`: Contains unit tests
- `/library/src/androidTest`: Contains instrumented tests
- `/docs`: Contains documentation for the project

## Libraries and Frameworks

- PowerAuth2 Mobile SDK for cryptographic operations.
- WultraPowerAuthNetworking for networking.

## Coding Standards

- `..editorconfig` file is used to enforce coding standards via ktlint
- The project follows Kotlin's naming conventions and best practices.

## Pull Request Guidelines

- Don't do any nitpick comments on pull requests.
- Don't write "Pull Request Overview" in the comment you generate.
- Don't describe the changes in the pull request description, don't write "This PR does X, Y, Z".
- Don't comment which files are you reviewing and which files you did not review.
- Focus on the functionality, architecture, and overall design of the code.
- Ensure that the code is well-documented (specially public APIs) and follows the project's coding standards
- Make sure that each change is documented in the `docs` documentation
- Make sure that tests are added or updated as necessary.
- Make sure that changelog is updated with the new version and changes.
- Check for grammar and spelling mistakes in the documentation and code comments.
