package org.asr.studio

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeJpeg(bytes: ByteArray): ImageBitmap?
