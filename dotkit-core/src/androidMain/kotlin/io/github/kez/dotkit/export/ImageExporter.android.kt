package io.github.kez.dotkit.export

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Android 플랫폼 이미지 내보내기 구현
 */
actual object ImageExporter {
    /**
     * PNG 이미지를 ByteArray로 내보내기
     */
    actual fun exportPNG(pixels: IntArray, width: Int, height: Int): ByteArray {
        return try {
            // Bitmap 생성
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

            // PNG로 압축
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

            bitmap.recycle()
            outputStream.toByteArray()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to export PNG: ${e.message}", e)
        }
    }

    /**
     * PNG 이미지를 Data URL로 내보내기
     */
    actual fun exportDataURL(pixels: IntArray, width: Int, height: Int): String {
        return try {
            val pngBytes = exportPNG(pixels, width, height)
            val base64 = Base64.encodeToString(pngBytes, Base64.NO_WRAP)
            "data:image/png;base64,$base64"
        } catch (e: Exception) {
            throw IllegalStateException("Failed to export Data URL: ${e.message}", e)
        }
    }
}
