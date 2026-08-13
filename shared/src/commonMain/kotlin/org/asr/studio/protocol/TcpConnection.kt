package org.asr.studio.protocol

import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TcpConnection(
    private val socket: Socket,
    private val scope: CoroutineScope
) {
    // Replay buffer prevents losing early packets (CAPABILITIES, HEARTBEAT)
    // if the StudioService collector hasn't reached it yet.
    private val _frames = MutableSharedFlow<AsrFrame>(replay = 64)
    val frames: SharedFlow<AsrFrame> = _frames.asSharedFlow()

    private val readChannel = socket.openReadChannel()
    private val writeChannel = socket.openWriteChannel(autoFlush = true)
    private val parser = PacketParser()
    
    private var readJob: Job? = null

    fun start() {
        println("[TcpConnection] start() called")
        readJob = scope.launch(Dispatchers.IO) {
            println("[TcpConnection] Read loop started")
            val buffer = ByteArray(65536) // 64KB read buffer for high-bandwidth JPEG camera stream
            try {
                while (true) {
                    val bytesRead = readChannel.readAvailable(buffer)
                    if (bytesRead == -1) {
                        println("[TcpConnection] readChannel reached EOF (-1). Client closed connection.")
                        break
                    }
                    if (bytesRead > 0) {
                        println("[TcpConnection] Received $bytesRead bytes")
                        val events = parser.accept(buffer.copyOfRange(0, bytesRead))
                        events.forEach { event ->
                            when (event) {
                                is PacketParserEvent.Packet -> {
                                    val f = event.frame
                                    println("[TcpConnection] Packet Decoded: Topic=${TopicId.fromValue(f.topicId)} Seq=${f.seq} Len=${f.payloadLength} CRC=${if(f.crcValid) "OK" else "FAIL"}")
                                    _frames.emit(f)
                                }
                                is PacketParserEvent.GarbageDiscarded -> {
                                    println("[TcpConnection] Garbage discarded: ${event.byteCount} bytes")
                                }
                                is PacketParserEvent.InvalidPacketLength -> {
                                    println("[TcpConnection] Invalid packet length: ${event.payloadLength}")
                                }
                                is PacketParserEvent.VersionMismatch -> {
                                    println("[TcpConnection] Version mismatch: expected ${event.expected}, actual ${event.actual}")
                                }
                                is PacketParserEvent.DecodeFailure -> {
                                    println("[TcpConnection] Decode failure (AsrPacket.decode returned null)")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("[TcpConnection] Read loop error: ${e.message}")
                e.printStackTrace()
            } finally {
                println("[TcpConnection] Read loop finally: closing socket and channels")
                runCatching { socket.close() }
            }
        }
    }

    suspend fun awaitCompletion() {
        readJob?.join()
    }

    suspend fun send(packet: AsrFrame) {
        println("[TcpConnection] send() packet: Topic=${TopicId.fromValue(packet.topicId)} Seq=${packet.seq}")
        val bytes = AsrPacket.encode(
            topicId = packet.topicId,
            seq = packet.seq,
            payload = packet.payload,
            flags = packet.flags,
            deviceId = packet.deviceId,
            timestampNs = packet.timestampNs
        )
        withContext(Dispatchers.IO) {
            writeChannel.writeFully(bytes)
        }
    }

    fun stop() {
        readJob?.cancel()
        socket.close()
    }
}
