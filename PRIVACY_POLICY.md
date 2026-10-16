# ASR Studio Privacy Policy

This policy describes ASR Studio, the desktop diagnostics application. It does not describe ASR Android Runtime or the optional ROS 2 USB Link Monitor.

## Data handled by Studio

When you connect Studio to an Android device, Studio receives the ASR packets sent to the TCP endpoint you configure. Depending on enabled Android modules, packet payloads can include camera frames, location, motion sensors, microphone levels, raw microphone audio, battery state, and device diagnostics. Studio displays and processes this information locally.

Studio keeps a rolling in-memory history of up to 1,000 received packets for its packet inspector and diagnostics. The packet history is not written to disk by Studio and is cleared when the application exits. Screens can also hold recent samples or the current camera frame in memory while they are open.

If you enable live speaker monitoring, raw microphone audio received from the phone is played through the computer's audio output. Studio sends user-requested controls and text-to-speech commands to the phone over the same TCP connection.

Studio does not include an analytics or telemetry upload service. Its TCP connection is directly between Studio and the endpoint you choose; Studio is not a cloud relay or ROS 2 DDS bridge. Connection and packet metadata and errors may be written to the process console.

## Preferences

Studio saves the light/dark theme choice in the local computer's Java Preferences store. The application does not save its packet history there.

## Network security

The ASR TCP protocol used by Studio does not provide application-level encryption or authentication. In listen mode, Studio defaults to binding to `0.0.0.0:4242`, which can make it reachable through the computer's network interfaces. Use a trusted network, configure the host and port deliberately, and stop listening when the connection is no longer needed. ADB reverse is an alternate USB route for the same TCP protocol; it does not add application-level encryption.

## External links

The optional Buy Me a Coffee button opens `https://buymeacoffee.com/shreyasdamase` in your browser. Studio does not process contributions or receive payment details. Buy Me a Coffee and any payment providers it uses handle information submitted on their site under their own policies.

## Changes and questions

This policy should be reviewed whenever Studio's data handling changes. For questions or corrections, use the project's [GitHub repository](https://github.com/ShreyasDamase/Asrstudio).
