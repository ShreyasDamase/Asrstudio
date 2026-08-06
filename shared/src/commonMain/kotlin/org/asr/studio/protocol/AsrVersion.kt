package org.asr.studio.protocol

object AsrVersion {
    const val PROTOCOL_VERSION: Byte = 0x01
    const val MAGIC_0: Byte = 0xA5.toByte()
    const val MAGIC_1: Byte = 0xAA.toByte()
    const val HEADER_SIZE = 28
    const val PAYLOAD_VERSION: Byte = 0x01
    const val RUNTIME_VERSION = "1.0.0"
    const val SPECIFICATION_VERSION = "1.0.0"
}
