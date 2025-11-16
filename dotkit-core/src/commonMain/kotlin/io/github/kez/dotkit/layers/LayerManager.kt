package io.github.kez.dotkit.layers

/**
 * 레이어 스택을 관리하는 클래스
 */
class LayerManager(
    private var layers: List<Layer> = emptyList()
) {
    /**
     * 현재 레이어 목록 (읽기 전용)
     */
    fun getLayers(): List<Layer> = layers

    /**
     * 레이어 개수
     */
    val size: Int get() = layers.size

    /**
     * 레이어가 비어있는지 확인
     */
    fun isEmpty(): Boolean = layers.isEmpty()

    /**
     * 새 레이어 추가 (맨 위에)
     */
    fun addLayer(layer: Layer): LayerManager {
        return LayerManager(layers + layer)
    }

    /**
     * 특정 인덱스에 레이어 삽입
     */
    fun insertLayer(index: Int, layer: Layer): LayerManager {
        require(index in 0..layers.size) { "Index out of bounds" }
        val newLayers = layers.toMutableList()
        newLayers.add(index, layer)
        return LayerManager(newLayers)
    }

    /**
     * 레이어 제거 (ID로)
     */
    fun removeLayer(layerId: String): LayerManager {
        return LayerManager(layers.filter { it.id != layerId })
    }

    /**
     * 레이어 제거 (인덱스로)
     */
    fun removeLayerAt(index: Int): LayerManager {
        require(index in layers.indices) { "Index out of bounds" }
        val newLayers = layers.toMutableList()
        newLayers.removeAt(index)
        return LayerManager(newLayers)
    }

    /**
     * 레이어 찾기 (ID로)
     */
    fun findLayer(layerId: String): Layer? {
        return layers.find { it.id == layerId }
    }

    /**
     * 레이어 인덱스 찾기
     */
    fun indexOf(layerId: String): Int {
        return layers.indexOfFirst { it.id == layerId }
    }

    /**
     * 레이어 업데이트
     */
    fun updateLayer(layerId: String, update: (Layer) -> Layer): LayerManager {
        val index = indexOf(layerId)
        if (index == -1) return this

        val newLayers = layers.toMutableList()
        newLayers[index] = update(newLayers[index])
        return LayerManager(newLayers)
    }

    /**
     * 레이어 순서 변경
     */
    fun moveLayer(fromIndex: Int, toIndex: Int): LayerManager {
        require(fromIndex in layers.indices) { "From index out of bounds" }
        require(toIndex in layers.indices) { "To index out of bounds" }

        if (fromIndex == toIndex) return this

        val newLayers = layers.toMutableList()
        val layer = newLayers.removeAt(fromIndex)
        newLayers.add(toIndex, layer)
        return LayerManager(newLayers)
    }

    /**
     * 레이어 복제
     */
    fun duplicateLayer(layerId: String): LayerManager {
        val layer = findLayer(layerId) ?: return this
        val index = indexOf(layerId)
        // Layer.create()의 generateId()를 사용하도록 수정
        val duplicated = Layer.create(
            width = layer.width,
            height = layer.height,
            name = "${layer.name} Copy"
        ).also {
            it.setPixels(layer.getPixelsCopy())
        }.copy(
            opacity = layer.opacity,
            visible = layer.visible,
            locked = layer.locked
        )
        return insertLayer(index + 1, duplicated)
    }

    /**
     * 모든 레이어를 합성하여 단일 픽셀 배열 생성
     * 하위 레이어부터 상위 레이어 순으로 알파 블렌딩
     */
    fun composite(width: Int, height: Int): IntArray {
        val result = IntArray(width * height)

        // 배경부터 전경까지 순서대로 합성
        for (layer in layers) {
            if (!layer.visible) continue

            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (layer.isInBounds(x, y)) {
                        val srcColor = layer.getPixel(x, y)
                        val srcAlpha = ((srcColor ushr 24) and 0xFF) / 255f * layer.opacity

                        if (srcAlpha > 0) {
                            val index = y * width + x
                            result[index] = blendPixels(result[index], srcColor, srcAlpha)
                        }
                    }
                }
            }
        }

        return result
    }

    /**
     * 두 픽셀을 알파 블렌딩
     */
    private fun blendPixels(dst: Int, src: Int, srcAlpha: Float): Int {
        if (srcAlpha <= 0f) return dst

        val dstA = ((dst ushr 24) and 0xFF) / 255f
        val dstR = (dst ushr 16) and 0xFF
        val dstG = (dst ushr 8) and 0xFF
        val dstB = dst and 0xFF

        val srcR = (src ushr 16) and 0xFF
        val srcG = (src ushr 8) and 0xFF
        val srcB = src and 0xFF

        val outA = srcAlpha + dstA * (1f - srcAlpha)

        if (outA < 0.001f) {
            return 0
        }

        val outR = (srcR * srcAlpha + dstR * dstA * (1f - srcAlpha)) / outA
        val outG = (srcG * srcAlpha + dstG * dstA * (1f - srcAlpha)) / outA
        val outB = (srcB * srcAlpha + dstB * dstA * (1f - srcAlpha)) / outA

        return ((outA * 255).toInt() shl 24) or
               (outR.toInt() shl 16) or
               (outG.toInt() shl 8) or
               outB.toInt()
    }
}
