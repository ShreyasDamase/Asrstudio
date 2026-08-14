package org.asr.studio.protocol

enum class TopicId(val value: Byte) {
    CAMERA_FRAME(0x01),
    IMU(0x02),
    GPS(0x03),
    BATTERY(0x04),
    AUDIO(0x05),
    ENVIRONMENT(0x06),
    CAPABILITIES(0x07),
    COMMAND(0x08),
    ACK(0x09),
    ERROR(0x0A),
    DIAGNOSTICS(0x0B),
    MOTION(0x0C),
    HEARTBEAT(0x0D),
    FLASHLIGHT(0x0E),
    LIGHT_SENSOR(0x0F),
    AUDIO_RAW(0x10),
    RESERVED_11(0x11),
    RESERVED_12(0x12),
    RESERVED_13(0x13),
    RESERVED_14(0x14),
    RESERVED_15(0x15),
    RESERVED_16(0x16),
    RESERVED_17(0x17),
    RESERVED_18(0x18),
    RESERVED_19(0x19),
    RESERVED_1A(0x1A),
    RESERVED_1B(0x1B),
    RESERVED_1C(0x1C),
    RESERVED_1D(0x1D),
    RESERVED_1E(0x1E),
    RESERVED_1F(0x1F);

    companion object {
        fun fromValue(value: Byte): TopicId? = entries.firstOrNull { it.value == value }
    }
}
