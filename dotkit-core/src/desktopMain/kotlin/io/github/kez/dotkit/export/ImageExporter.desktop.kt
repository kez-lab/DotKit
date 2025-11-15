package io.github.kez.dotkit.export

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

/**
 * Desktop (JVM) 플랫폼 이미지 내보내기 구현
 */
actual object ImageExporter {
    /**
     * PNG 이미지를 ByteArray로 내보내기
     */
    actual fun exportPNG(pixels: IntArray, width: Int, height: Int): ByteArray {
        return try {
            // BufferedImage 생성
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            image.setRGB(0, 0, width, height, pixels, 0, width)

            // PNG로 인코딩
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(image, "PNG", outputStream)

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
            val base64 = Base64.getEncoder().encodeToString(pngBytes)
            "data:image/png;base64,$base64"
        } catch (e: Exception) {
            throw IllegalStateException("Failed to export Data URL: ${e.message}", e)
        }
    }
}
