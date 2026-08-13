package org.asr.studio

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun runAdbReverseAutoSetup(port: Int = 4242): String