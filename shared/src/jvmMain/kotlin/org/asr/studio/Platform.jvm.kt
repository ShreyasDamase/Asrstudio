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
            "✅ Auto-configured: adb reverse tcp:$port tcp:$port"
        } else {
            "⚠️ ADB error: ${error.ifBlank { output }}"
        }
    } catch (e: Exception) {
        "⚠️ Could not run adb command: ${e.message}"
    }
}
