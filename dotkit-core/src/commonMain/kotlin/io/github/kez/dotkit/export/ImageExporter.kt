package io.github.kez.dotkit.export

/**
 * 플랫폼별 이미지 내보내기 인터페이스
 *
 * expect/actual 패턴을 사용하여 각 플랫폼에 최적화된 내보내기를 제공합니다.
 */
expect object ImageExporter {
    /**
     * PNG 이미지를 ByteArray로 내보내기
     *
     * Android, Desktop에서 사용합니다.
     *
     * @param pixels ARGB 픽셀 데이터 (IntArray)
     * @param width 이미지 너비
     * @param height 이미지 높이
     * @return PNG 형식의 ByteArray
     */
    fun exportPNG(pixels: IntArray, width: Int, height: Int): ByteArray

    /**
     * PNG 이미지를 Data URL로 내보내기 (Web 전용)
     *
     * @param pixels ARGB 픽셀 데이터 (IntArray)
     * @param width 이미지 너비
     * @param height 이미지 높이
     * @return base64 인코딩된 Data URL 문자열
     */
    fun exportDataURL(pixels: IntArray, width: Int, height: Int): String
}

/**
 * 이미지 내보내기 옵션
 */
data class ExportOptions(
    val format: ImageFormat = ImageFormat.PNG,
    val quality: Int = 100, // 0-100 (JPEG, WebP용)
    val scale: Float = 1f   // 출력 스케일 (1.0 = 원본 크기)
) {
    init {
        require(quality in 0..100) { "Quality must be between 0 and 100" }
        require(scale > 0) { "Scale must be positive" }
    }
}

/**
 * 지원되는 이미지 형식
 */
enum class ImageFormat {
    PNG,
    JPEG,
    WEBP
}

/**
 * 내보내기 결과
 */
sealed class ExportResult {
    /**
     * 성공 (ByteArray)
     */
    data class ByteArrayResult(val data: ByteArray) : ExportResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ByteArrayResult

            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }

    /**
     * 성공 (Data URL)
     */
    data class DataURLResult(val url: String) : ExportResult()

    /**
     * 실패
     */
    data class Error(val message: String, val cause: Throwable? = null) : ExportResult()
}
