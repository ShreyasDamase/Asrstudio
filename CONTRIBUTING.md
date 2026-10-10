# Contributing to ASR Studio

Thanks for your interest in improving ASR Studio. Before submitting a change, check the current [roadmap](ROADMAP.md) and open an issue or discussion for substantial product or protocol changes.

## Development setup

- JDK 17 or later
- Git
- Android Debug Bridge only if validating USB/ADB forwarding

Build and launch from the repository root:

```bash
./gradlew :desktopApp:assemble
./gradlew :desktopApp:run
```

## Change guidelines

- Keep changes focused and preserve unrelated working-tree changes.
- Follow the existing Kotlin and Compose patterns; keep screen code with its feature.
- Avoid machine-specific paths, generated build outputs, IDE state, and crash dumps in commits.
- Keep setup and behavior documentation aligned with the application and companion Android Runtime.
- Separate ASR Studio TCP behavior from ROS 2 DDS behavior in code and documentation.
- For protocol changes, verify compatibility against the Android Runtime implementation before changing parser or command behavior.
- Include build or manual verification details in the pull request. Clearly label checks that were not performed.

## Pull requests

Describe the user-visible change, the reason for it, how it was verified, and any known limitations. Include screenshots for material UI changes where possible. Do not include secrets, personal machine paths, or device identifiers in examples.

## License

ASR Studio is distributed under the MIT License. See [LICENSE](LICENSE) and [NOTICE.txt](NOTICE.txt) for the project license and third-party notices.
