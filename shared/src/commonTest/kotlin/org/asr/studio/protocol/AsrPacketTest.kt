package org.asr.studio.protocol

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AsrPacketTest {
    @Test
    fun encodesAndDecodesAndroidCompatibleHeader() {
        val payload = byteArrayOf(AsrVersion.PAYLOAD_VERSION, 0x10, 0x20)
        val packet = AsrPacket.encode(
            topicId = TopicId.IMU.value,
            seq = 42,
            payload = payload,
            flags = PacketFlags.NONE,
            deviceId = 7,
            timestampNs = 123456789L,
        )

        assertEquals(AsrVersion.HEADER_SIZE + payload.size, packet.size)
        assertEquals(AsrVersion.MAGIC_0, packet[0])
        assertEquals(AsrVersion.MAGIC_1, packet[1])
        assertEquals(AsrVersion.PROTOCOL_VERSION, packet[2])
        assertEquals(TopicId.IMU.value, packet[3])
        assertEquals(PacketFlags.NONE, packet[4])
        assertEquals(7, packet[5].toInt())
        assertEquals(123456789L, AsrPacket.readLongLe(packet, 8))
        assertEquals(42, AsrPacket.readIntLe(packet, 16))
        assertEquals(payload.size, AsrPacket.readIntLe(packet, 20))

        val decoded = assertNotNull(AsrPacket.decode(packet))
        assertEquals(TopicId.IMU.value, decoded.topicId)
        assertEquals(42, decoded.seq)
        assertEquals(123456789L, decoded.timestampNs)
        assertContentEquals(payload, decoded.payload)
        assertTrue(decoded.crcValid)
    }

    @Test
    fun rejectsShortOrWrongMagicPackets() {
        assertNull(AsrPacket.decode(byteArrayOf(0x01, 0x02)))

        val payload = byteArrayOf(AsrVersion.PAYLOAD_VERSION)
        val packet = AsrPacket.encode(
            topicId = TopicId.HEARTBEAT.value,
            seq = 1,
            payload = payload,
            timestampNs = 1L,
        )
        packet[0] = 0x00

        assertNull(AsrPacket.decode(packet))
    }

    @Test
    fun reportsCrcFailuresWithoutDroppingHeaderFields() {
        val packet = AsrPacket.encode(
            topicId = TopicId.GPS.value,
            seq = 9,
            payload = byteArrayOf(AsrVersion.PAYLOAD_VERSION, 0x01),
            deviceId = 3,
            timestampNs = 99L,
        )
        packet[AsrVersion.HEADER_SIZE] = 0x7F

        val decoded = assertNotNull(AsrPacket.decode(packet))
        assertEquals(TopicId.GPS.value, decoded.topicId)
        assertEquals(9, decoded.seq)
        assertEquals(3, decoded.deviceId.toInt())
        assertFalse(decoded.crcValid)
    }
}
