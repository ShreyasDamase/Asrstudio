# ASR Studio Roadmap

This roadmap reflects the current desktop application. It is a planning document, not a promise of a release date.

## Current capabilities

- [x] Compose Multiplatform desktop application shell and feature navigation.
- [x] ASR TCP connection over Wi-Fi and USB/ADB reverse forwarding.
- [x] Camera, IMU, GPS, microphone, speaker, dashboard, and packet inspection screens.
- [x] IMU orientation viewport with live device rotation, compass directions, view presets, and reset controls.
- [x] In-app getting-started, connectivity, protocol, ROS 2, and sensor documentation.
- [x] Separate Privacy Policy and Buy Me a Coffee sidebar destinations.
- [x] About page with the desktop app version.

## Release readiness

- [x] Add a repository license selected by the maintainers.
- [x] Audit bundled assets and dependencies; include required third-party license and notice files.
- [ ] Review contribution, issue-reporting, and security-reporting guidance for the public repository.
- [x] Confirm maintainer/creator credit and public contact/project links for the About page and README.
- [x] Add Studio-specific privacy information; verify Android Runtime URLs and protocol details against its source without editing that repository.
- [ ] Build and inspect native packages on each intended operating system.
- [ ] Validate TCP setup against the current Android release and document known limitations.
- [ ] Capture release screenshots and provide concise installation instructions.

## Follow-up product work

- [x] Review the Logs navigation entry and either implement it or clearly mark its availability in the UI.
- [ ] Continue checking responsive layout and text hierarchy on smaller desktop windows.
- [ ] Keep in-app protocol and sensor references synchronized with the Android runtime.
- [ ] Improve automated coverage for packet decoding and state transformations as the public release workflow is established.

## Related projects

- [ASR Android Runtime](https://github.com/ShreyasDamase/asr-android-runtime) provides the phone runtime and ROS 2 publishers.
- [ASR ROS 2 USB Link Monitor](https://github.com/ShreyasDamase/asr-ros2-link-monitor) provides optional USB-tether DDS diagnostics.
