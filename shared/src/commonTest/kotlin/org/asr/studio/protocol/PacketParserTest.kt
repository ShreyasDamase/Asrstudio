package org.asr.studio.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PacketParserTest {
    @Test
    fun waitsForSplitPackets() {
        val parser = PacketParser()
        val packet = packet(seq = 1)

        assertTrue(parser.accept(packet.copyOfRange(0, 10)).isEmpty())

        val events = parser.accept(packet.copyOfRange(10, packet.size))
        val parsed = assertIs<PacketParserEvent.Packet>(events.single())
        assertEquals(1, parsed.frame.seq)
    }

    @Test
    fun parsesMergedPacketsFromSingleRead() {
        val parser = PacketParser()
        val first = packet(seq = 1)
        val second = packet(seq = 2, topic = TopicId.GPS)

        val events = parser.accept(first + second)

        assertEquals(2, events.size)
        assertEquals(1, assertIs<PacketParserEvent.Packet>(events[0]).frame.seq)
        assertEquals(2, assertIs<PacketParserEvent.Packet>(events[1]).frame.seq)
        assertEquals(TopicId.GPS.value, assertIs<PacketParserEvent.Packet>(events[1]).frame.topicId)
    }

    @Test
    fun discardsGarbageBeforeMagicBytesAndResynchronizes() {
        val parser = PacketParser()
        val events = parser.accept(byteArrayOf(0x00, 0x01, 0x02) + packet(seq = 3))

        assertEquals(PacketParserEvent.GarbageDiscarded(3), events[0])
        assertEquals(3, assertIs<PacketParserEvent.Packet>(events[1]).frame.seq)
    }

    @Test
    fun keepsTrailingMagicPrefixForNextRead() {
        val parser = PacketParser()

        val firstEvents = parser.accept(byteArrayOf(0x00, AsrVersion.MAGIC_0))
        assertEquals(PacketParserEvent.GarbageDiscarded(1), firstEvents.single())

        val secondEvents = parser.accept(byteArrayOf(AsrVersion.MAGIC_1) + packet(seq = 4).drop(2).toByteArray())
        assertEquals(4, assertIs<PacketParserEvent.Packet>(secondEvents.single()).frame.seq)
    }

    @Test
    fun emitsPacketsWithCrcFailureAndContinuesParsing() {
        val parser = PacketParser()
        val bad = packet(seq = 5)
        bad[AsrVersion.HEADER_SIZE] = 0x55
        val good = packet(seq = 6)

        val events = parser.accept(bad + good)

        val badPacket = assertIs<PacketParserEvent.Packet>(events[0])
        val goodPacket = assertIs<PacketParserEvent.Packet>(events[1])
        assertEquals(5, badPacket.frame.seq)
        assertEquals(false, badPacket.frame.crcValid)
        assertEquals(6, goodPacket.frame.seq)
        assertEquals(true, goodPacket.frame.crcValid)
    }

    @Test
    fun emitsVersionMismatchAndConsumesThePacket() {
        val parser = PacketParser()
        val mismatched = packet(seq = 7)
        mismatched[2] = 0x02
        val crc = AsrPacket.computeCrc(mismatched.copyOf(24), mismatched.copyOfRange(AsrVersion.HEADER_SIZE, mismatched.size))
        mismatched[24] = (crc and 0xFF).toByte()
        mismatched[25] = ((crc ushr 8) and 0xFF).toByte()
        mismatched[26] = ((crc ushr 16) and 0xFF).toByte()
        mismatched[27] = ((crc ushr 24) and 0xFF).toByte()

        val events = parser.accept(mismatched + packet(seq = 8))

        assertEquals(0x02, assertIs<PacketParserEvent.VersionMismatch>(events[0]).actual)
        assertEquals(8, assertIs<PacketParserEvent.Packet>(events[1]).frame.seq)
    }

    @Test
    fun invalidNegativeLengthDropsOneByteAndResynchronizes() {
        val parser = PacketParser()
        val invalid = packet(seq = 9)
        invalid[20] = 0xFF.toByte()
        invalid[21] = 0xFF.toByte()
        invalid[22] = 0xFF.toByte()
        invalid[23] = 0xFF.toByte()

        val events = parser.accept(invalid + packet(seq = 10))

        assertEquals(PacketParserEvent.InvalidPacketLength(-1), events[0])
        assertTrue(events.any { it is PacketParserEvent.Packet && it.frame.seq == 10 })
    }

    @Test
    fun resetDropsPartialPacketAfterDisconnect() {
        val parser = PacketParser()
        val packet = packet(seq = 11)

        assertTrue(parser.accept(packet.copyOfRange(0, 12)).isEmpty())
        parser.reset()

        val events = parser.accept(packet(seq = 12))
        assertEquals(12, assertIs<PacketParserEvent.Packet>(events.single()).frame.seq)
    }

    private fun packet(seq: Int, topic: TopicId = TopicId.IMU): ByteArray =
        AsrPacket.encode(
            topicId = topic.value,
            seq = seq,
            payload = byteArrayOf(AsrVersion.PAYLOAD_VERSION, seq.toByte()),
            deviceId = 1,
            timestampNs = seq.toLong(),
        )
}
