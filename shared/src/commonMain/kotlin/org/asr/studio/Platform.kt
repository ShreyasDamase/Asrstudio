package org.asr.studio

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun runAdbReverseAutoSetup(port: Int = 4242): String

/**
 * Run any `adb shell am start-foreground-service ...` command from the desktop.
 * Returns the combined stdout/stderr output from adb.
 */
expect fun runAdbShell(vararg args: String): String

/**
 * Loads authentic 3D CAD assets (.obj, .mtl) directly from the project or resources.
 */
expect fun loadModelAsset(filename: String): String

/**
 * Plays real-time desktop audio monitor tone/beep on macOS/JVM.
 */
expect fun playMonitorFeedback(active: Boolean)

/**
 * Streams real 16 kHz 16-bit PCM audio samples directly to the desktop speaker (SourceDataLine).
 */
expect fun playAudioPcm(pcmBytes: ByteArray, offset: Int, length: Int)