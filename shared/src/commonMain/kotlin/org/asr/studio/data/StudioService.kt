package org.asr.studio.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.asr.studio.currentTimeMillis
import org.asr.studio.protocol.TcpClient

class StudioService {
    companion object {
        val instance by lazy { StudioService() }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    val tcpClient = TcpClient(scope)
    val packetRepository = PacketRepository()
    val sensorRepository = SensorRepository()

    init {
        scope.launch {
            tcpClient.frames.collect { frame ->
                val now = currentTimeMillis()
                packetRepository.addPacket(frame, now)
                sensorRepository.processPacket(frame)
            }
        }
        
        scope.launch {
            tcpClient.connectionState.collectLatest { state ->
                // If disconnected, we might want to clear some transient state
                // but packet history might be useful to keep.
            }
        }
    }
}
