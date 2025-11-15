package io.github.kez.dotkit.canvas

/**
 * 픽셀 데이터를 관리하는 버퍼 클래스
 * ARGB 포맷으로 픽셀을 저장하고 관리합니다.
 *
 * @param width 버퍼의 너비 (픽셀 단위)
 * @param height 버퍼의 높이 (픽셀 단위)
 */
class PixelBuffer(
    val width: Int,
    val height: Int
) {
    /**
     * ARGB 패킹된 정수 배열로 픽셀 데이터 저장
     * 각 픽셀은 32비트 정수로 표현: [AARRGGBB]
     */
    private val pixels: IntArray = IntArray(width * height)

    init {
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
    }

    /**
     * 특정 좌표의 픽셀 색상 가져오기
     *
     * @param x X 좌표 (0부터 width-1까지)
     * @param y Y 좌표 (0부터 height-1까지)
     * @return ARGB 패킹된 색상 값
     * @throws IndexOutOfBoundsException 좌표가 범위를 벗어난 경우
     */
    fun getPixel(x: Int, y: Int): Int {
        checkBounds(x, y)
        return pixels[y * width + x]
    }

    /**
     * 특정 좌표에 픽셀 색상 설정
     *
     * @param x X 좌표 (0부터 width-1까지)
     * @param y Y 좌표 (0부터 height-1까지)
     * @param color ARGB 패킹된 색상 값
     * @throws IndexOutOfBoundsException 좌표가 범위를 벗어난 경우
     */
    fun setPixel(x: Int, y: Int, color: Int) {
        checkBounds(x, y)
        pixels[y * width + x] = color
    }

    /**
     * 좌표가 유효한 범위 내에 있는지 확인
     *
     * @param x X 좌표
     * @param y Y 좌표
     * @return 유효한 좌표인 경우 true, 아니면 false
     */
    fun isInBounds(x: Int, y: Int): Boolean {
        return x in 0 until width && y in 0 until height
    }

    /**
     * 모든 픽셀을 특정 색상으로 채우기
     *
     * @param color ARGB 패킹된 색상 값
     */
    fun fill(color: Int) {
        pixels.fill(color)
    }

    /**
     * 모든 픽셀을 투명하게 초기화
     */
    fun clear() {
        fill(0x00000000)
    }

    /**
     * 픽셀 데이터의 복사본 가져오기
     *
     * @return 픽셀 배열의 복사본
     */
    fun getPixelsCopy(): IntArray {
        return pixels.copyOf()
    }

    /**
     * 다른 버퍼의 픽셀 데이터를 현재 버퍼에 복사
     * 두 버퍼의 크기가 같아야 합니다.
     *
     * @param other 복사할 소스 버퍼
     * @throws IllegalArgumentException 버퍼 크기가 다른 경우
     */
    fun copyFrom(other: PixelBuffer) {
        require(width == other.width && height == other.height) {
            "Buffer dimensions must match"
        }
        other.pixels.copyInto(pixels)
    }

    /**
     * 픽셀 데이터 배열을 직접 설정
     * 배열 크기가 width * height와 일치해야 합니다.
     *
     * @param data 설정할 픽셀 데이터
     * @throws IllegalArgumentException 배열 크기가 맞지 않는 경우
     */
    fun setPixels(data: IntArray) {
        require(data.size == pixels.size) {
            "Data array size must match buffer size (${pixels.size})"
        }
        data.copyInto(pixels)
    }

    /**
     * 좌표 범위 검사
     *
     * @param x X 좌표
     * @param y Y 좌표
     * @throws IndexOutOfBoundsException 좌표가 범위를 벗어난 경우
     */
    private fun checkBounds(x: Int, y: Int) {
        if (!isInBounds(x, y)) {
            throw IndexOutOfBoundsException(
                "Pixel coordinates ($x, $y) are out of bounds (0..$width, 0..$height)"
            )
        }
    }

    companion object {
        /**
         * ARGB 색상 값 생성
         *
         * @param alpha 알파 (0-255)
         * @param red 빨강 (0-255)
         * @param green 녹색 (0-255)
         * @param blue 파랑 (0-255)
         * @return ARGB 패킹된 색상 값
         */
        fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
            return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }

        /**
         * RGB 색상 값 생성 (불투명)
         *
         * @param red 빨강 (0-255)
         * @param green 녹색 (0-255)
         * @param blue 파랑 (0-255)
         * @return ARGB 패킹된 색상 값 (알파 = 255)
         */
        fun rgb(red: Int, green: Int, blue: Int): Int {
            return argb(255, red, green, blue)
        }

        /**
         * ARGB 색상에서 알파 추출
         */
        fun getAlpha(color: Int): Int = (color shr 24) and 0xFF

        /**
         * ARGB 색상에서 빨강 추출
         */
        fun getRed(color: Int): Int = (color shr 16) and 0xFF

        /**
         * ARGB 색상에서 녹색 추출
         */
        fun getGreen(color: Int): Int = (color shr 8) and 0xFF

        /**
         * ARGB 색상에서 파랑 추출
         */
        fun getBlue(color: Int): Int = color and 0xFF
    }
}
