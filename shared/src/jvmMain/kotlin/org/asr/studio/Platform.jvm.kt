package org.asr.studio

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun runAdbReverseAutoSetup(port: Int): String {
    return try {
        // Remove stale/conflicting forward rules
        runCatching { ProcessBuilder("adb", "forward", "--remove-all").start().waitFor() }
        val pb = ProcessBuilder("adb", "reverse", "tcp:$port", "tcp:$port")
        val process = pb.start()
        val output = process.inputStream.bufferedReader().readText().trim()
        val error = process.errorStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()
        if (exitCode == 0) {
            "Configured: adb reverse tcp:$port tcp:$port"
        } else {
            "ADB error: ${error.ifBlank { output }}"
        }
    } catch (e: Exception) {
        "Could not run adb command: ${e.message}"
    }
}

actual fun runAdbShell(vararg args: String): String {
    return try {
        val cmd = listOf("adb", "shell") + args.toList()
        val pb = ProcessBuilder(cmd).redirectErrorStream(true)
        val process = pb.start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        output.ifBlank { "Command sent" }
    } catch (e: Exception) {
        "ADB error: ${e.message}"
    }
}

actual fun loadModelAsset(filename: String): String {
    val candidates = listOf(
        java.io.File(filename),
        java.io.File("shared/src/commonMain/composeResources/files/$filename"),
        java.io.File("../$filename")
    )
    for (f in candidates) {
        if (f.exists()) return f.readText()
    }
    val stream = JVMPlatform::class.java.classLoader.getResourceAsStream(filename)
        ?: JVMPlatform::class.java.classLoader.getResourceAsStream("files/$filename")
    return stream?.bufferedReader()?.readText() ?: ""
}

actual fun playMonitorFeedback(active: Boolean) {
    // Silent mode or visual
}

private var audioLine: javax.sound.sampled.SourceDataLine? = null

actual fun playAudioPcm(pcmBytes: ByteArray, offset: Int, length: Int) {
    try {
        if (audioLine == null) {
            val format = javax.sound.sampled.AudioFormat(
                16000.0f,  // 16 kHz sample rate
                16,        // 16-bit
                1,         // Mono
                true,      // Signed PCM
                false      // Little Endian
            )
            val info = javax.sound.sampled.DataLine.Info(javax.sound.sampled.SourceDataLine::class.java, format)
            if (javax.sound.sampled.AudioSystem.isLineSupported(info)) {
                val line = javax.sound.sampled.AudioSystem.getLine(info) as javax.sound.sampled.SourceDataLine
                line.open(format, 4096)
                line.start()
                audioLine = line
            }
        }
        audioLine?.write(pcmBytes, offset, length)
    } catch (e: Exception) {
        // Output device busy or unavailable
    }
}

private val studioPreferences = java.util.prefs.Preferences.userRoot().node("org/asr/studio")

actual fun loadStudioThemePreference(): Boolean = studioPreferences.getBoolean("darkTheme", true)

actual fun saveStudioThemePreference(isDarkTheme: Boolean) {
    studioPreferences.putBoolean("darkTheme", isDarkTheme)
}
