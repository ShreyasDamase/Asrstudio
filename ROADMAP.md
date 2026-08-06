# ASR Studio Roadmap

Implementation happens one phase at a time. Do not start a later phase until the current phase is complete and approved.

## Phase 1 - Foundation

Status: Complete

- [x] Compose Desktop project
- [x] Gradle configuration
- [x] Material 3 theme
- [x] Project structure
- [x] Navigation
- [x] Application shell: sidebar, title bar, status bar
- [x] Placeholder screens
- [x] Successful build verification

Explicitly out of scope:

- Networking
- Protocol parsing
- Repositories
- Diagnostics
- Persistence
- Android project modifications

## Phase 2 - Protocol Discovery

Status: In progress

- [x] Inspect `cameraros` protocol classes
- [x] Compare Android implementation with written specification
- [x] Document packet layout, constants, flags, CRC, command IDs
- [ ] Ask for clarification on conflicts

Finding: Android `TcpTransport` is a TCP client that connects outward to a configured host and port. The Phase 2 instructions also request an ASR Studio `TcpClient`. This creates a connection-direction conflict that must be clarified before implementing the socket layer.

## Phase 3 - Protocol Models

Status: Not started

- [ ] Implement ASR Studio protocol models
- [ ] Implement binary-compatible parsing and serialization
- [ ] Add compatibility tests based on Android behavior

## Phase 4 - Transport Foundation

Status: Not started

- [ ] Implement TCP transport
- [ ] Define connection state
- [ ] Add connection UI state
- [ ] Add transport tests where practical

## Phase 5 - Diagnostics Views

Status: Not started

- [ ] Camera diagnostics
- [ ] IMU diagnostics
- [ ] GPS diagnostics
- [ ] Protocol inspector
- [ ] Logs

## Phase 6 - Documentation And Packaging

Status: Not started

- [ ] Embedded documentation
- [ ] Native desktop packaging
- [ ] Installation verification
