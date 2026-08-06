package org.asr.studio.protocol

object PacketFlags {
    const val NONE: Byte = 0x00
    const val COMPRESSED: Byte = 0x01
    const val FRAGMENTED: Byte = 0x02
    const val ERROR: Byte = 0x04
    const val END_OF_FRAME: Byte = 0x08
}

enum class CameraEncoding(val id: Byte) {
    RAW_YUV(0x00),
    JPEG(0x01),
    H264(0x02),
}
