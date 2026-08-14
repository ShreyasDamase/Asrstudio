package org.asr.studio.ui.speaker

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.asr.studio.speech.SpeechNaturaliser
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

    private val _naturalMode = MutableStateFlow(false)
    val naturalMode: StateFlow<Boolean> = _naturalMode.asStateFlow()

    fun setNaturalMode(enabled: Boolean) { _naturalMode.value = enabled }

    fun speak(text: String) {
        if (text.isNotBlank()) {
            if (_naturalMode.value) {
                val ssml = SpeechNaturaliser.naturalise(text.trim())
                speakerRepository.speak(ssml, isSSML = true)
            } else {
                speakerRepository.speak(text.trim(), isSSML = false)
            }
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
