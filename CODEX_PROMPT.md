# CodeX Implementation Instructions

This file contains CodeX-specific workflow instructions for ASR Studio. The primary engineering contract is `AGENTS.md`; read and follow it before making changes.

## Required Reading Order

1. `AGENTS.md`
2. `ARCHITECTURE.md`
3. `ROADMAP.md`
4. Relevant source files for the active phase
5. `/Users/shreyasdamase/Vanguard/cameraros` only when protocol compatibility is relevant

## Operating Rules

- Implement one roadmap phase at a time.
- Keep work scoped to the approved phase.
- Ask before resolving ambiguity.
- Do not modify `/Users/shreyasdamase/Vanguard/cameraros`.
- Do not implement protocol, networking, diagnostics, persistence, or command handling during Phase 1.
- Run the build before reporting a phase complete.
- If build failures expose an architectural ambiguity, stop and ask instead of inventing a workaround.

## Phase 1 Direction

Phase 1 establishes the application foundation:

- Compose Desktop project
- Gradle configuration
- Material 3 theme
- Project structure
- Navigation
- Application shell with sidebar, title bar, and status bar
- Placeholder screens
- Successful build verification

The shell should be functional but non-operational: navigation may switch between placeholders, but no runtime data flow should be implemented.

## Compatibility Rule

When later phases involve protocol compatibility, inspect `cameraros` first and compare it with the written specification. If the Android implementation differs from the written specification, report the difference and ask which version is canonical.
