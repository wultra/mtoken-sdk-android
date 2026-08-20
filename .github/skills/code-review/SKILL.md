---
name: code-review
description: Review pull requests in the Mobile Token SDK for Android. Use when reviewing Kotlin APIs, operations, OIDC, push, serialization, security, or release changes.
---

# Mobile Token SDK Android code review

Review `mtoken-sdk-android` as the Maven-published `com.wultra.android:mtoken-sdk` Android SDK. Before deciding, verify the repository, PR target/head, and local commit with `git remote -v`, `git branch --show-current`, `git status --short`, and `git log -1 --oneline`. The expected integration base is `develop`; identify `release/*` bases explicitly and do not infer a PR from the checkout alone.

## Decision and communication

- Approve by default. Raise only demonstrated correctness, security, public-compatibility, or required-release-documentation defects.
- A finding must contain an affected changed `path:line`, a concrete consumer/security impact, and an actionable correction. Do not write speculative findings.
- Do not comment on formatting, style, naming, refactoring preferences, CI setup, or unproven missing tests.
- Never send GitHub reviews, comments, or other posts without explicit user approval. Any explicitly approved postable content starts with `🤖`.
- Review grammar only in changed public `README.md`, `docs/**/*.md`, or public KDoc/Javadoc, and only if the base is not `release/*`.

## Modules and public contracts

`library` is the only published module, with namespace `com.wultra.android.mtokensdk`, manifest `library/src/main/AndroidManifest.xml`, and consumer rules `library/consumer-proguard-rules.pro`. Public API is rooted at `library/src/main/java/com/wultra/android/mtokensdk/`; scrutinize compatibility of `WultraMobileToken.kt`, `operation/OperationsService.kt` and `IOperationsServiceListener.kt`, `inbox/InboxService.kt`, `oidc/OIDCService.kt`, and `push/PushService.kt` / `PushParser.kt`.

For transport/model behavior, trace changes through `api/operation/OperationApi.kt`, `IOperationApi.kt`, operation serializers/deserializers and `api/operation/model/**`; `api/inbox/InboxApi.kt` and `api/inbox/model/**`; `api/oidc/OIDCApi.kt` and `oidc/models/**`; and `api/push/PushApi.kt` / `api/push/model/**`. Serialized JSON keys, optional/null/default handling, date parsing, QR parser semantics, rejection/authorization payloads, and error conversion are wire contracts—flag only confirmed server or app incompatibilities.

Asynchronous services must preserve listener/completion behavior: each started operation completes exactly once, all success, HTTP, parsing, cancellation, and PowerAuth failures reach the documented result/listener path, and callbacks retain their expected dispatch semantics. Changes to OIDC authorization/PKCE (`oidc/**`), push registration/parsing (`push/**`), or operation authorization/rejection/pre-approval (`operation/**`, `api/operation/**`) require special attention to token leakage, state/nonce/verifier continuity, and untrusted payload validation. Logs in `log/WMTLogger.kt` / `WMTLogListener.kt` must not expose credentials, tokens, authorization data, QR payloads, or personal operation contents.

## Versions, release material, documentation, and tests

`library/gradle.properties` is the authoritative `VERSION_NAME`; `library/build.gradle.kts` exposes it in `BuildConfig`. On a release-to-`develop` change, every declared development version must be `0.0.1-dev`, including that properties file and changed release metadata. Release automation is `.prepare-release.json` and `scripts/prepare-release.sh`.

Public integration material is `README.md` and `docs/` (especially `SDK-Integration.md`, `Example-Usage.md`, `Using-Operations-Service.md`, `Using-OIDC-Service.md`, `Using-Push-Service.md`, `Using-Inbox-Service.md`, `Error-Handling.md`, and `Changelog.md`). Require corresponding docs and a changelog entry only for an externally observable API, integration, or behavior change. Follow the existing contribution guidance in `.github/CONTRIBUTING.md`; project-specific commands and constraints are in `.github/copilot-instructions.md`. Workflows `.github/workflows/{build,lint,tests}.yml` and scripts `scripts/{build-and-publish,lint,prepare-release}.sh` are evidence sources, not subjects for CI advice.

Use focused tests: JVM tests in `library/src/test/java/` cover JSON, QR parsing, push, authorization, and PAC utilities; instrumentation tests in `library/src/androidTest/java/` cover operations, OIDC, inbox, expiration, and integration. Run only relevant existing Gradle tests when validation is required.
