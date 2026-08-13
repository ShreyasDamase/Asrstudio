package org.asr.studio.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.asr.studio.protocol.AsrFrame
import org.asr.studio.protocol.TopicId
import kotlin.math.max

data class PacketEntry(
    val frame: AsrFrame,
    val receptionTimeMs: Long
)

data class Diagnostics(
    val totalPackets: Long = 0,
    val totalBytes: Long = 0,
    val packetRate: Double = 0.0,
    val byteRate: Double = 0.0,
    val dropCount: Long = 0,
    val lastHeartbeatTimeMs: Long = 0,
    val topicCounts: Map<TopicId, Long> = emptyMap()
)

class PacketRepository(private val capacity: Int = 1000) {
    private val _packets = MutableStateFlow<List<PacketEntry>>(emptyList())
    val packets: StateFlow<List<PacketEntry>> = _packets.asStateFlow()

    private val _diagnostics = MutableStateFlow(Diagnostics())
    val diagnostics: StateFlow<Diagnostics> = _diagnostics.asStateFlow()

    private val ringBuffer = ArrayDeque<PacketEntry>(capacity)
    
    // Per-topic last sequence number — ASR uses independent counters per topic,
    // so we must NOT compare sequence numbers across different topics.
    private val lastSeqByTopic = mutableMapOf<Byte, Int>()
    private var firstPacketTime: Long? = null
    
    @Synchronized
    fun addPacket(frame: AsrFrame, receptionTimeMs: Long) {
        val entry = PacketEntry(frame, receptionTimeMs)
        
        // Ring buffer management
        if (ringBuffer.size >= capacity) {
            ringBuffer.removeFirst()
        }
        ringBuffer.addLast(entry)
        _packets.value = ringBuffer.toList()

        // Diagnostics update
        val currentDiag = _diagnostics.value
        val newTotalPackets = currentDiag.totalPackets + 1
        val newTotalBytes = currentDiag.totalBytes + (28 + frame.payloadLength)
        
        val newTopicCounts = currentDiag.topicCounts.toMutableMap()
        val topic = TopicId.fromValue(frame.topicId) ?: TopicId.RESERVED_1F
        newTopicCounts[topic] = (newTopicCounts[topic] ?: 0L) + 1
        
        var newDropCount = currentDiag.dropCount
        val topicKey = frame.topicId
        val prevSeq = lastSeqByTopic[topicKey]
        if (prevSeq != null) {
            val gap = frame.seq - (prevSeq + 1)
            if (gap > 0) {
                newDropCount += gap
            }
        }
        lastSeqByTopic[topicKey] = frame.seq

        var lastHeartbeat = currentDiag.lastHeartbeatTimeMs
        if (topic == TopicId.HEARTBEAT) {
            lastHeartbeat = receptionTimeMs
        }

        if (firstPacketTime == null) firstPacketTime = receptionTimeMs
        val durationSec = max(1.0, (receptionTimeMs - firstPacketTime!!).toDouble() / 1000.0)
        
        _diagnostics.value = Diagnostics(
            totalPackets = newTotalPackets,
            totalBytes = newTotalBytes,
            packetRate = newTotalPackets / durationSec,
            byteRate = newTotalBytes / durationSec,
            dropCount = newDropCount,
            lastHeartbeatTimeMs = lastHeartbeat,
            topicCounts = newTopicCounts
        )
    }

    fun clear() {
        ringBuffer.clear()
        _packets.value = emptyList()
        _diagnostics.value = Diagnostics()
        lastSeqByTopic.clear()
        firstPacketTime = null
    }
}
