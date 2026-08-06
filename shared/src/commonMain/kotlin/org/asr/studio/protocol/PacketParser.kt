package org.asr.studio.protocol

class PacketParser {
    private var buffer = ByteArray(4096)
    private var bufferedSize = 0

    fun accept(bytes: ByteArray): List<PacketParserEvent> {
        println("[PacketParser] accept() ${bytes.size} bytes. Total buffered: ${bufferedSize + bytes.size}")
        append(bytes)
        val events = mutableListOf<PacketParserEvent>()

        while (true) {
            val magicIndex = findMagicIndex()
            if (magicIndex < 0) {
                if (bufferedSize > 1) {
                    val discarded = bufferedSize - 1
                    println("[PacketParser] No magic found, discarding $discarded bytes")
                    buffer[0] = buffer[bufferedSize - 1]
                    bufferedSize = 1
                    events += PacketParserEvent.GarbageDiscarded(discarded)
                }
                return events
            }

            if (magicIndex > 0) {
                println("[PacketParser] Magic found at $magicIndex, discarding prefix of $magicIndex bytes")
                consume(magicIndex)
                events += PacketParserEvent.GarbageDiscarded(magicIndex)
            }

            if (bufferedSize < AsrVersion.HEADER_SIZE) {
                println("[PacketParser] Partial header: $bufferedSize/${AsrVersion.HEADER_SIZE}")
                return events
            }

            val payloadLen = AsrPacket.readIntLe(buffer, 20)
            println("[PacketParser] Header found. PayloadLength specified: $payloadLen")
            
            if (payloadLen < 0 || payloadLen > 10 * 1024 * 1024) { // Max 10MB sanity check
                println("[PacketParser] REJECTED: Invalid payload length: $payloadLen")
                consume(1)
                events += PacketParserEvent.InvalidPacketLength(payloadLen)
                continue
            }

            val frameSize = AsrVersion.HEADER_SIZE + payloadLen
            if (bufferedSize < frameSize) {
                println("[PacketParser] Partial packet. Need $frameSize bytes, have $bufferedSize")
                return events
            }

            println("[PacketParser] Full packet buffered. size=$frameSize. Decoding...")
            val packetBytes = buffer.copyOfRange(0, frameSize)
            consume(frameSize)

            val frame = AsrPacket.decode(packetBytes)
            if (frame == null) {
                println("[PacketParser] REJECTED: AsrPacket.decode() returned null")
                events += PacketParserEvent.DecodeFailure(packetBytes)
                continue
            }

            if (frame.version != AsrVersion.PROTOCOL_VERSION) {
                println("[PacketParser] REJECTED: Version mismatch: expected ${AsrVersion.PROTOCOL_VERSION}, actual ${frame.version}")
                events += PacketParserEvent.VersionMismatch(
                    expected = AsrVersion.PROTOCOL_VERSION,
                    actual = frame.version,
                    rawPacket = packetBytes,
                    frame = frame,
                )
                continue
            }

            if (!frame.crcValid) {
                println("[PacketParser] REJECTED: CRC mismatch: stored=0x${frame.storedCrc.toString(16).uppercase()} computed=0x${frame.computedCrc.toString(16).uppercase()}")
                // IMPORTANT: Step 3 - If rejected, print WHY. 
                // Do not emit the packet if CRC is invalid to prevent corrupted ROS messages.
                events += PacketParserEvent.DecodeFailure(packetBytes)
                continue
            }

            println("[PacketParser] SUCCESS: Packet Decoded. Topic=${TopicId.fromValue(frame.topicId)} Seq=${frame.seq} size=${frameSize} ts=${frame.timestampNs}")
            events += PacketParserEvent.Packet(frame = frame, rawPacket = packetBytes)
        }
    }

    fun reset() {
        bufferedSize = 0
    }

    private fun append(bytes: ByteArray) {
        ensureCapacity(bufferedSize + bytes.size)
        bytes.copyInto(buffer, destinationOffset = bufferedSize)
        bufferedSize += bytes.size
    }

    private fun ensureCapacity(required: Int) {
        if (required <= buffer.size) return
        var newSize = buffer.size
        while (newSize < required) newSize *= 2
        buffer = buffer.copyOf(newSize)
    }

    private fun findMagicIndex(): Int {
        var index = 0
        while (index < bufferedSize - 1) {
            if (buffer[index] == AsrVersion.MAGIC_0 && buffer[index + 1] == AsrVersion.MAGIC_1) {
                return index
            }
            index++
        }
        return -1
    }

    private fun consume(count: Int) {
        if (count <= 0) return
        if (count >= bufferedSize) {
            bufferedSize = 0
            return
        }
        buffer.copyInto(
            destination = buffer,
            destinationOffset = 0,
            startIndex = count,
            endIndex = bufferedSize,
        )
        bufferedSize -= count
    }
}

sealed interface PacketParserEvent {
    data class Packet(
        val frame: AsrFrame,
        val rawPacket: ByteArray,
    ) : PacketParserEvent {
        override fun equals(other: Any?): Boolean =
            other is Packet &&
                frame == other.frame &&
                rawPacket.contentEquals(other.rawPacket)

        override fun hashCode(): Int = 31 * frame.hashCode() + rawPacket.contentHashCode()
    }

    data class VersionMismatch(
        val expected: Byte,
        val actual: Byte,
        val rawPacket: ByteArray,
        val frame: AsrFrame,
    ) : PacketParserEvent {
        override fun equals(other: Any?): Boolean =
            other is VersionMismatch &&
                expected == other.expected &&
                actual == other.actual &&
                rawPacket.contentEquals(other.rawPacket) &&
                frame == other.frame

        override fun hashCode(): Int {
            var result = expected.toInt()
            result = 31 * result + actual
            result = 31 * result + rawPacket.contentHashCode()
            result = 31 * result + frame.hashCode()
            return result
        }
    }

    data class GarbageDiscarded(val byteCount: Int) : PacketParserEvent

    data class InvalidPacketLength(val payloadLength: Int) : PacketParserEvent

    data class DecodeFailure(val rawPacket: ByteArray) : PacketParserEvent {
        override fun equals(other: Any?): Boolean =
            other is DecodeFailure && rawPacket.contentEquals(other.rawPacket)

        override fun hashCode(): Int = rawPacket.contentHashCode()
    }
}
