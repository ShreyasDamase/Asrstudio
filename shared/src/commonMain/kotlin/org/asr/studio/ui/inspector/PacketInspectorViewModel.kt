package org.asr.studio.ui.inspector

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.asr.studio.data.PacketEntry
import org.asr.studio.data.StudioService

class PacketInspectorViewModel(
    private val studioService: StudioService
) : ViewModel() {
    val packets: StateFlow<List<PacketEntry>> = studioService.packetRepository.packets

    fun clear() {
        studioService.packetRepository.clear()
    }
}
