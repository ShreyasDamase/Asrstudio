# ASR Studio Engineering Contract

This file is the authoritative engineering contract for every AI coding agent working on ASR Studio. Read it before making changes.

## Source Of Truth

Priority order:

1. Explicit user instructions
2. The existing Android ASR implementation in `/Users/shreyasdamase/Vanguard/cameraros`
3. The written ASR Studio architecture specification

If these conflict, stop and ask for clarification. Do not resolve conflicts by guessing.

## Existing Project Context

The Android Sensor Runtime (ASR) is already implemented in a separate repository.

- Repository name: `cameraros`
- Local path: `/Users/shreyasdamase/Vanguard/cameraros`
- Role: reference implementation and protocol authority

ASR Studio must remain protocol compatible with `cameraros`. Before implementing networking, protocol, packet parsing, serialization, command handling, or diagnostics, inspect the Android project and understand the relevant implementation.

Important Android classes may include:

- `AsrPacket`
- `AsrVersion`
- `PacketSerializer`
- `TopicId`
- `PacketFlags`
- `TcpTransport`
- `CommandDispatcher`
- `CameraPipeline`
- `GpsManager`
- `TransportManager`
- `DevModeConfig`

Class names may vary. Search the Android project when needed.

## ASR Repository Policy

Treat `/Users/shreyasdamase/Vanguard/cameraros` as read-only.

You are not authorized to:

- Edit any file inside the ASR repository
- Rename classes
- Change the protocol
- Modify packet formats
- Change topic IDs
- Change CRC implementation
- Change serialization
- Change Android architecture
- Add new Android features
- Fix Android code without approval
- Refactor Android code
- Commit changes to ASR

If you believe ASR needs to change:

1. Explain what should change.
2. Explain why.
3. Explain the impact.
4. List every file that would require modification.
5. Wait for explicit approval.

ASR Studio must adapt to the existing Android implementation unless the user explicitly decides otherwise.

## Repository Boundary

ASR Studio is a sibling repository, not a dependency of `cameraros`.

- Keep repositories independent.
- Keep ASR Studio changes inside this repository.
- Do not create dependencies that require modifying the Android project.
- The Android project must continue to build and function exactly as it does today.
- Reuse protocol, networking concepts, constants, packet layout, CRC behavior, payload parsing rules, serialization rules, command IDs, and shared data models where appropriate.
- Do not copy Android UI architecture.

## Communication Rules

Do not assume architecture, behavior, protocol, or implementation details that are not explicitly specified.

If anything is ambiguous:

1. Stop.
2. Explain what is ambiguous.
3. Present the possible options.
4. Recommend one option with technical reasoning.
5. Wait for approval before implementing.

Never silently choose an implementation.

Ask before proceeding if unsure about:

- Architecture
- Folder structure
- Package names
- API design
- Binary protocol
- Networking behavior
- Threading model
- State management
- UI/UX behavior
- Naming conventions
- Database schema
- Performance optimizations
- Future extensibility
- Build configuration
- Dependency selection
- Third-party libraries
- Platform-specific behavior
- Error handling
- Security decisions
- Anything not explicitly defined in the specification

## Never Guess

Never write code based on assumptions.

Bad:

- "I assumed the camera should reconnect automatically."
- "I assumed the repository should cache packets."
- "I assumed packet history should be stored in SQLite."
- "I assumed this command should return JSON."

Instead ask:

"The specification does not define camera reconnect behavior. Should reconnect be automatic, manual, or configurable?"

## Never Invent Features

Do not add features because they seem useful.

Examples of forbidden unapproved additions:

- Auto-update system
- Plugin marketplace
- Telemetry upload
- Cloud synchronization
- Analytics
- User accounts
- Authentication
- Themes beyond the specification
- Hidden debug APIs
- AI assistants
- Experimental protocol extensions

Only implement features defined in the specification or explicitly approved.

## Architecture Rules

Do not:

- Rename packages
- Merge modules
- Split modules
- Replace MVVM
- Replace Compose
- Replace Ktor
- Replace Material 3
- Replace StateFlow
- Introduce another architectural pattern

unless the user explicitly approves the change.

If you believe part of the architecture is technically weak:

1. Explain why.
2. Describe the risks.
3. Suggest an alternative.
4. Wait for approval.

Do not implement the alternative without approval.

## Decision Protocol

For every significant architectural decision:

1. State the problem.
2. Explain the available options.
3. Recommend one option.
4. Wait for approval.
5. Implement only after approval.

## Development Workflow

Before implementing any feature:

1. Read `AGENTS.md`.
2. Read `ARCHITECTURE.md`.
3. Read the Android reference implementation in `/Users/shreyasdamase/Vanguard/cameraros` if protocol compatibility is required.
4. Ask for clarification if anything is ambiguous.
5. Implement only one roadmap phase at a time.
6. Ensure the project builds successfully before considering the phase complete.

## Coding Standards

- Use Kotlin and Compose Multiplatform Desktop.
- Prefer existing project patterns over new abstractions.
- Keep changes scoped to the active roadmap phase.
- Use Material 3 for UI.
- Use `StateFlow` for view model state when stateful view models are introduced.
- Do not add dependencies unless required by the approved specification or explicitly approved.
- Keep protocol code binary compatible with `cameraros`.
- Add focused tests for logic with meaningful behavior.

## Phase Gate

Phase 1 is foundation only:

- Compose Desktop project
- Gradle configuration
- Material 3 theme
- Project structure
- Navigation
- Application shell
- Placeholder screens
- Successful build

Do not implement networking, protocol parsing, repositories, persistence, diagnostics, or runtime integrations until Phase 1 is complete and the next phase is approved.
