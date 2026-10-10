# ASR Studio

ASR Studio is the desktop diagnostics and visualization application for [ASR Android Runtime](https://github.com/ShreyasDamase/asr-android-runtime). It connects to the phone over TCP to display camera, sensor, audio, and runtime telemetry.

ASR Studio's TCP connection and ROS 2 DDS are different paths. The desktop application does not act as a DDS bridge. For ROS 2 discovery and USB sensor-flow diagnostics, use the companion [ASR ROS 2 USB Link Monitor](https://github.com/ShreyasDamase/asr-ros2-link-monitor).

## Features

- TCP connection over Wi-Fi or USB / ADB forwarding.
- Live camera and sensor telemetry with packet and sequence diagnostics.
- IMU orientation visualization with phone-frame axes, view presets, and pose readouts.
- Audio, speaker, GPS, device, and protocol diagnostics.
- In-app guides for setup, connectivity, sensors, and ROS 2 topics.

## Downloads

| Version | OS | Asset |
| ------- | -- | ----- |
| 1.0.0 | macOS | [ASR Studio macOS Release](https://github.com/ShreyasDamase/asr-studio/releases/latest) |
| 1.0.0 | Windows | [ASR Studio Windows Release](https://github.com/ShreyasDamase/asr-studio/releases/latest) |
| 1.0.0 | Linux | [ASR Studio Linux Release](https://github.com/ShreyasDamase/asr-studio/releases/latest) |


## Requirements

- JDK 17 or later.
- Android phone running ASR Android Runtime.
- For USB / ADB TCP, Android Debug Bridge and an authorized USB debugging connection.
- For DDS, a separate ROS 2 host and the USB helper when monitoring a USB-tethered link. The helper is not needed for Wi-Fi DDS.

## Build and launch

From the repository root:

```bash
./gradlew :desktopApp:assemble
./gradlew :desktopApp:run
```

The desktop distribution version is `1.0.0`. Open **About** in the app sidebar to see the application version and purpose.

## Connect ASR Studio to the phone

Open **Devices** in ASR Studio. The default TCP port is `4242`.

### Wi-Fi TCP

1. Connect the phone and computer to the same reachable network.
2. Select **Wi-Fi**, enter the computer's local IP address, and select **Listen (Server)**.
3. In the Android app's TCP section, enable TCP and enter the computer IP and port `4242`.

### USB / ADB TCP

1. Connect the phone over USB and authorize USB debugging.
2. Select **USB / ADB** and click **Auto-Configure USB / ADB (1-Click Setup)**. This runs `adb reverse tcp:4242 tcp:4242`.
3. Select **Listen (Server)** in ASR Studio.
4. Enable TCP on Android and connect to `127.0.0.1:4242`.

This ADB forwarding path is for ASR Studio TCP. ROS 2 USB DDS instead uses Android USB tethering and the USB-only ROS 2 helper.

## ROS 2 DDS

The Android app publishes DDS topics under `/asr_android_<device-id>`. The host and phone must use the same `ROS_DOMAIN_ID` (default `0`). The tested host configuration uses ROS 2 Lyrical and `rmw_fastrtps_cpp`; Android uses Cyclone DDS.

- **Wi-Fi DDS:** enable Wi-Fi DDS in the phone app and use a normal ROS 2 subscriber or robot node on the network. No ASR helper is needed.
- **USB DDS:** enable USB tethering in Android settings, enable USB DDS in the app, and use the [USB Link Monitor guide](https://github.com/ShreyasDamase/asr-ros2-link-monitor) for host setup and troubleshooting.

## Documentation

- [Android Runtime setup and ROS 2 topic reference](https://github.com/ShreyasDamase/asr-android-runtime#readme)
- [USB Link Monitor setup and diagnostic interpretation](https://github.com/ShreyasDamase/asr-ros2-link-monitor#readme)
- [Architecture](ARCHITECTURE.md)
- [Roadmap](ROADMAP.md)
- [Contributing](CONTRIBUTING.md)

## Source and licensing

This project is maintained by [Shreyas Damase](https://github.com/ShreyasDamase).

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. Third-party licenses and attribution notices are provided in the [NOTICE.txt](NOTICE.txt) file.
