package io.github.kez.dotkit.export

import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

/**
 * iOS 플랫폼 이미지 내보내기 구현
 */
@OptIn(ExperimentalForeignApi::class)
actual object ImageExporter {
    /**
     * PNG 이미지를 ByteArray로 내보내기
     */
    actual fun exportPNG(pixels: IntArray, width: Int, height: Int): ByteArray {
        return try {
            val uiImage = createUIImage(pixels, width, height)
            val pngData = UIImagePNGRepresentation(uiImage)
                ?: throw IllegalStateException("Failed to create PNG representation")

            // NSData를 ByteArray로 변환
            pngData.toByteArray()
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
            val base64 = pngBytes.toNSData().base64EncodedStringWithOptions(0u)
            "data:image/png;base64,$base64"
        } catch (e: Exception) {
            throw IllegalStateException("Failed to export Data URL: ${e.message}", e)
        }
    }

    /**
     * IntArray 픽셀 데이터로부터 UIImage 생성
     */
    private fun createUIImage(pixels: IntArray, width: Int, height: Int): UIImage {
        val colorSpace = CGColorSpaceCreateDeviceRGB()
        val bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst.value or
                (1u shl 12) // kCGBitmapByteOrder32Little

        // 픽셀 데이터를 네이티브 메모리에 복사
        val pixelData = pixels.usePinned { pinned ->
            val size = width * height * 4
            val data = nativeHeap.allocArray<ByteVar>(size)
            val srcPtr = pinned.addressOf(0).reinterpret<ByteVar>()
            for (i in 0 until size) {
                data[i] = srcPtr[i]
            }
            data
        }

        // CGContext 생성
        val context = CGBitmapContextCreate(
            data = pixelData,
            width = width.toULong(),
            height = height.toULong(),
            bitsPerComponent = 8u,
            bytesPerRow = (width * 4).toULong(),
            space = colorSpace,
            bitmapInfo = bitmapInfo.toUInt()
        ) ?: throw IllegalStateException("Failed to create CGBitmapContext")

        // CGImage 생성
        val cgImage = CGBitmapContextCreateImage(context)
            ?: throw IllegalStateException("Failed to create CGImage")

        // UIImage 생성
        val uiImage = UIImage.imageWithCGImage(cgImage)

        // 메모리 해제
        nativeHeap.free(pixelData)

        return uiImage
    }
}

/**
 * NSData를 ByteArray로 변환
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.toByteArray(): ByteArray {
    return ByteArray(this.length.toInt()).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }
}

/**
 * ByteArray를 NSData로 변환
 */
@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}
