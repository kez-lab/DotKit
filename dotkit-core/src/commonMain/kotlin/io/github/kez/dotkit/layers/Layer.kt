package io.github.kez.dotkit.layers

import io.github.kez.dotkit.canvas.PixelBuffer

/**
 * 캔버스의 레이어를 나타내는 데이터 클래스
 *
 * @param id 레이어의 고유 식별자
 * @param name 레이어 이름
 * @param width 레이어 너비 (픽셀)
 * @param height 레이어 높이 (픽셀)
 * @param opacity 레이어 불투명도 (0.0 ~ 1.0)
 * @param visible 레이어 가시성
 * @param locked 레이어 잠금 상태
 */
data class Layer(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val opacity: Float = 1f,
    val visible: Boolean = true,
    val locked: Boolean = false
) {
    /**
     * 레이어의 픽셀 데이터
     */
    private val buffer: PixelBuffer = PixelBuffer(width, height)

    init {
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
        require(opacity in 0f..1f) { "Opacity must be between 0.0 and 1.0" }
    }

    /**
     * 특정 좌표의 픽셀 색상 가져오기
     */
    fun getPixel(x: Int, y: Int): Int = buffer.getPixel(x, y)

    /**
     * 특정 좌표에 픽셀 색상 설정
     */
    fun setPixel(x: Int, y: Int, color: Int) {
        if (!locked) {
            buffer.setPixel(x, y, color)
        }
    }

    /**
     * 좌표가 유효한 범위 내에 있는지 확인
     */
    fun isInBounds(x: Int, y: Int): Boolean = buffer.isInBounds(x, y)

    /**
     * 레이어를 특정 색상으로 채우기
     */
    fun fill(color: Int) {
        if (!locked) {
            buffer.fill(color)
        }
    }

    /**
     * 레이어를 투명하게 초기화
     */
    fun clear() {
        if (!locked) {
            buffer.clear()
        }
    }

    /**
     * 픽셀 데이터의 복사본 가져오기
     */
    fun getPixelsCopy(): IntArray = buffer.getPixelsCopy()

    /**
     * 픽셀 데이터 설정
     */
    fun setPixels(data: IntArray) {
        if (!locked) {
            buffer.setPixels(data)
        }
    }

    /**
     * 레이어 복사본 생성
     */
    fun copy(
        id: String = this.id,
        name: String = this.name,
        opacity: Float = this.opacity,
        visible: Boolean = this.visible,
        locked: Boolean = this.locked
    ): Layer {
        return Layer(id, name, width, height, opacity, visible, locked).also {
            it.setPixels(this.getPixelsCopy())
        }
    }

    companion object {
        // 전역 카운터로 고유 ID 보장
        private var idCounter = 0L

        /**
         * 새 빈 레이어 생성
         */
        fun create(
            width: Int,
            height: Int,
            name: String = "Layer",
            id: String = generateId()
        ): Layer {
            return Layer(
                id = id,
                name = name,
                width = width,
                height = height
            )
        }

        /**
         * 고유 ID 생성
         * 랜덤 값 + 카운터로 충돌 방지
         */
        private fun generateId(): String {
            val random = kotlin.random.Random.nextLong()
            val counter = idCounter++
            return "layer_${random}_${counter}"
        }
    }
}
