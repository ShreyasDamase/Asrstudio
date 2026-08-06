# Contributing To ASR Studio

Read `AGENTS.md` before making changes.

## Workflow

1. Work on one roadmap phase at a time.
2. Keep changes scoped to the approved phase.
3. Ask for clarification when behavior or architecture is ambiguous.
4. Run the relevant Gradle build before considering work complete.
5. Do not modify `/Users/shreyasdamase/Vanguard/cameraros` without explicit approval.

## Build

Run:

```bash
./gradlew build
```

Run desktop app:

```bash
./gradlew :desktopApp:run
```

## Coding Conventions

- Kotlin code follows standard Kotlin style.
- Compose UI should use Material 3 components.
- Keep UI state explicit and easy to inspect.
- Use MVVM with `StateFlow` when feature view models are introduced.
- Prefer small, focused files over large mixed-responsibility files.
- Do not introduce new dependencies without approval or a clear specification requirement.

## Testing

- Add tests for parsing, serialization, state reducers, and other deterministic logic.
- Protocol tests must be based on the Android reference implementation where possible.
- UI placeholder-only changes do not require protocol tests.

## Commit Messages

Use concise, imperative commit messages:

```text
Add phase one app shell
Document ASR repository policy
Implement protocol header parser
```

## Branch Strategy

Use focused branches by phase or feature. Avoid mixing unrelated documentation, architecture, and implementation changes unless they are part of the approved phase.
