package org.asr.studio.ui.speaker

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.asr.studio.data.SpeakerLogEvent
import org.asr.studio.data.SpeakerRepository
import org.asr.studio.data.SpeakerTelemetryData
import org.asr.studio.data.StudioService

class SpeakerViewModel(
    private val studioService: StudioService = StudioService.instance,
    private val speakerRepository: SpeakerRepository = studioService.speakerRepository
) : ViewModel() {

    val telemetry: StateFlow<SpeakerTelemetryData> = speakerRepository.telemetry
    val logEvents: StateFlow<List<SpeakerLogEvent>> = speakerRepository.logEvents

    fun speak(text: String) {
        if (text.isNotBlank()) {
            speakerRepository.speak(text.trim())
        }
    }

    fun testVoice() {
        speak("ASR speaker test successful")
    }

    fun testWarning() {
        speak("Warning. Obstacle detected ahead.")
    }

    fun testSequence() {
        speak("Counting down: 3. 2. 1. Launch!")
    }
}
