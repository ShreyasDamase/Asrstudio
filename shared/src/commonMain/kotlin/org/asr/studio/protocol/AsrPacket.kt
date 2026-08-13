package org.asr.studio.protocol

data class AsrFrame(
    val version: Byte,
    val topicId: Byte,
    val flags: Byte,
    val deviceId: Byte,
    val timestampNs: Long,
    val seq: Int,
    val payloadLength: Int,
    val storedCrc: Long,
    val computedCrc: Long,
    val payload: ByteArray,
    val crcValid: Boolean,
) {
    override fun equals(other: Any?): Boolean =
        other is AsrFrame &&
            version == other.version &&
            topicId == other.topicId &&
            flags == other.flags &&
            deviceId == other.deviceId &&
            timestampNs == other.timestampNs &&
            seq == other.seq &&
            payloadLength == other.payloadLength &&
            storedCrc == other.storedCrc &&
            computedCrc == other.computedCrc &&
            payload.contentEquals(other.payload) &&
            crcValid == other.crcValid

    override fun hashCode(): Int {
        var result = version.toInt()
        result = 31 * result + topicId
        result = 31 * result + flags
        result = 31 * result + deviceId
        result = 31 * result + timestampNs.hashCode()
        result = 31 * result + seq
        result = 31 * result + payloadLength
        result = 31 * result + storedCrc.hashCode()
        result = 31 * result + computedCrc.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + crcValid.hashCode()
        return result
    }
}

object AsrPacket {
    fun encode(
        topicId: Byte,
        seq: Int,
        payload: ByteArray,
        flags: Byte = PacketFlags.NONE,
        deviceId: Byte = 0,
        timestampNs: Long,
    ): ByteArray {
        val bytes = ByteArray(AsrVersion.HEADER_SIZE + payload.size)
        bytes[0] = AsrVersion.MAGIC_0
        bytes[1] = AsrVersion.MAGIC_1
        bytes[2] = AsrVersion.PROTOCOL_VERSION
        bytes[3] = topicId
        bytes[4] = flags
        bytes[5] = deviceId
        writeShortLe(bytes, 6, 0)
        writeLongLe(bytes, 8, timestampNs)
        writeIntLe(bytes, 16, seq)
        writeIntLe(bytes, 20, payload.size)

        payload.copyInto(bytes, destinationOffset = AsrVersion.HEADER_SIZE)
        val crc = computeCrc(bytes.copyOf(24), payload)
        writeIntLe(bytes, 24, crc.toInt())
        return bytes
    }

    fun decode(bytes: ByteArray): AsrFrame? {
        if (bytes.size < AsrVersion.HEADER_SIZE) return null
        if (bytes[0] != AsrVersion.MAGIC_0 || bytes[1] != AsrVersion.MAGIC_1) return null

        val payloadLen = readIntLe(bytes, 20)
        if (payloadLen < 0) return null
        if (bytes.size < AsrVersion.HEADER_SIZE + payloadLen) return null

        val payload = bytes.copyOfRange(AsrVersion.HEADER_SIZE, AsrVersion.HEADER_SIZE + payloadLen)
        val storedCrc = readUIntLe(bytes, 24)
        val computedCrc = computeCrc(bytes.copyOf(24), payload)

        return AsrFrame(
            version = bytes[2],
            topicId = bytes[3],
            flags = bytes[4],
            deviceId = bytes[5],
            timestampNs = readLongLe(bytes, 8),
            seq = readIntLe(bytes, 16),
            payloadLength = payloadLen,
            storedCrc = storedCrc,
            computedCrc = computedCrc,
            payload = payload,
            crcValid = storedCrc == computedCrc,
        )
    }

    fun computeCrc(header0to23: ByteArray, payload: ByteArray): Long {
        // Standard IEEE 802.3 CRC32 — matches Java's java.util.zip.CRC32 exactly:
        //   Polynomial = 0xEDB88320
        //   Initial    = 0xFFFFFFFF
        //   Final XOR  = 0xFFFFFFFF (.inv())
        var crc = 0xFFFFFFFF.toInt()
        fun update(bytes: ByteArray) {
            for (b in bytes) {
                crc = crc xor (b.toInt() and 0xFF)
                repeat(8) {
                    crc = if ((crc and 1) != 0) {
                        (crc ushr 1) xor 0xEDB88320.toInt()
                    } else {
                        crc ushr 1
                    }
                }
            }
        }
        update(header0to23)
        update(payload)
        return (crc.inv().toLong()) and 0xFFFFFFFFL
    }

    internal fun readIntLe(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    internal fun readLongLe(bytes: ByteArray, offset: Int): Long {
        var value = 0L
        for (index in 0 until 8) {
            value = value or ((bytes[offset + index].toLong() and 0xFFL) shl (8 * index))
        }
        return value
    }

    private fun readUIntLe(bytes: ByteArray, offset: Int): Long =
        readIntLe(bytes, offset).toLong() and 0xFFFFFFFFL

    private fun writeShortLe(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }

    private fun writeIntLe(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private fun writeLongLe(bytes: ByteArray, offset: Int, value: Long) {
        for (index in 0 until 8) {
            bytes[offset + index] = ((value ushr (8 * index)) and 0xFF).toByte()
        }
    }
}
