# Repository Guidance

These notes apply to work in ASR Studio. User instructions and safety constraints take precedence.

## Project boundaries

- ASR Studio is a Kotlin/Compose Multiplatform desktop diagnostics client.
- The Android Runtime and ROS 2 USB Link Monitor are maintained in separate repositories. Do not modify those repositories as part of an ASR Studio change unless the user explicitly asks for cross-repository work.
- When a change depends on Android packet, command, or topic behavior, verify it against the companion Android Runtime source or its published documentation when available. Do not invent protocol details.
- TCP telemetry to ASR Studio and ROS 2 DDS are separate paths. The Studio is not a DDS bridge.

## Implementation practices

- Follow the existing Kotlin, Compose, and Gradle patterns in the affected module.
- Keep UI components and feature logic organized by screen/domain under `shared/src/commonMain/kotlin/org/asr/studio/ui/`.
- Keep platform-specific code in the relevant source set and avoid machine-specific absolute paths.
- Keep the About version synchronized with `desktopApp/build.gradle.kts` native package version.
- Do not introduce dependencies without a clear feature need; explain material trade-offs.
- Do not add analytics, telemetry upload, authentication, or other unrelated features.
- Preserve existing user changes and unrelated work in the working tree.

## Verification

- For Kotlin or Gradle changes, run the narrowest relevant build task when practical and report its result.
- Do not claim UI behavior was visually verified unless the app was launched and inspected.
- Add or update tests when requested or when a change introduces deterministic logic with meaningful correctness risk.

## Documentation and public distribution

- Keep root README, in-app documentation, and companion-project instructions consistent with current behavior.
- Clearly distinguish verified behavior from expected or untested behavior.
- Do not claim the repository is open source until a license has been selected and added.
- Preserve third-party attribution and license notices for bundled assets and dependencies.
