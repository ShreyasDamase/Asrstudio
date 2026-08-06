# ASR Studio

ASR Studio is a Compose Multiplatform Desktop application for diagnostics, visualization, and documentation for the Android Sensor Runtime ecosystem.

The Android runtime lives in a sibling repository:

- `cameraros`: `/Users/shreyasdamase/Vanguard/cameraros`

`cameraros` is the protocol authority. ASR Studio must remain binary compatible with it and must not modify it.

## Current Status

Phase 1 foundation is in progress.

Implemented scope:

- Compose Desktop project foundation
- Material 3 application shell
- Local navigation
- Placeholder screens

Not implemented yet:

- Networking
- Protocol parsing
- Diagnostics
- Persistence
- Runtime integrations

## Quick Start

Build the project:

```bash
./gradlew build
```

Run the desktop app:

```bash
./gradlew :desktopApp:run
```

## Documentation

- `AGENTS.md`: engineering contract for AI coding agents
- `CODEX_PROMPT.md`: CodeX-specific workflow
- `ARCHITECTURE.md`: system architecture overview
- `ROADMAP.md`: implementation phases
- `CONTRIBUTING.md`: coding standards and contribution workflow
