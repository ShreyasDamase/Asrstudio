package org.asr.studio.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.asr.studio.currentTimeMillis
import org.asr.studio.protocol.AsrFrame
import org.asr.studio.protocol.AsrVersion
import org.asr.studio.protocol.TopicId

data class SpeakerLogEvent(
    val timestampMs: Long,
    val text: String,
    val status: String
)

data class SpeakerTelemetryData(
    val status: String = "RUNNING",
    val ttsEngine: String = "Google TTS",
    val language: String = "en-US",
    val volume: Int = 80,
    val commandsSent: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val lastSpeechText: String = "-",
    val lastResult: String = "NONE",
    val lastLatencyMs: Long = 0L
)

class SpeakerRepository(
    private val studioServiceSupplier: () -> StudioService,
    private val scope: CoroutineScope
) {
    private val _telemetry = MutableStateFlow(SpeakerTelemetryData())
    val telemetry: StateFlow<SpeakerTelemetryData> = _telemetry.asStateFlow()

    private val _logEvents = MutableStateFlow<List<SpeakerLogEvent>>(emptyList())
    val logEvents: StateFlow<List<SpeakerLogEvent>> = _logEvents.asStateFlow()

    fun speak(text: String) {
        val now = currentTimeMillis()
        val current = _telemetry.value
        val newSent = current.commandsSent + 1

        _telemetry.value = current.copy(
            commandsSent = newSent,
            completedCount = current.completedCount + 1,
            lastSpeechText = text,
            lastResult = "SUCCESS",
            lastLatencyMs = (12..25).random().toLong()
        )

        val newEvent = SpeakerLogEvent(
            timestampMs = now,
            text = text,
            status = "SPEAK SUCCESS"
        )
        _logEvents.value = (listOf(newEvent) + _logEvents.value).take(50)

        // Send JSON command frame over TCP
        val reqId = 3000 + newSent
        val json = """{"cmd":"speaker.speak","params":{"text":"${text.replace("\"", "'")}"},"req_id":$reqId}"""
        val payload = json.encodeToByteArray()
        val frame = AsrFrame(
            version = AsrVersion.PROTOCOL_VERSION,
            topicId = TopicId.COMMAND.value,
            flags = 0,
            deviceId = 0,
            timestampNs = now * 1_000_000,
            seq = newSent,
            payloadLength = payload.size,
            storedCrc = 0,
            computedCrc = 0,
            payload = payload,
            crcValid = true
        )
        scope.launch(Dispatchers.IO) {
            studioServiceSupplier().tcpClient.send(frame)
        }
    }
}
