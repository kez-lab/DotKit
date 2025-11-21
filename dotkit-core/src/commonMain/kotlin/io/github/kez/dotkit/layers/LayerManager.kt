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
        val size = width * height

        // 배경부터 전경까지 순서대로 합성
        for (layer in layers) {
            if (!layer.visible) continue

            // 레이어 크기가 캔버스 크기와 같고 오프셋이 없는 경우 (일반적인 경우) 최적화
            // 현재 Layer 구조상 오프셋은 없으므로 크기만 확인
            if (layer.width == width && layer.height == height) {
                val layerPixels = layer.getPixelsCopy() // Note: This copies, but access is faster. Ideally we'd access internal buffer.
                
                for (i in 0 until size) {
                    val srcColor = layerPixels[i]
                    val srcAlpha = (srcColor ushr 24) and 0xFF
                    
                    if (srcAlpha == 0) continue
                    
                    // If fully opaque, just overwrite (unless layer opacity < 1f)
                    if (srcAlpha == 255 && layer.opacity >= 1f) {
                        result[i] = srcColor
                    } else {
                        val finalAlpha = srcAlpha / 255f * layer.opacity
                        if (finalAlpha > 0) {
                            result[i] = blendPixels(result[i], srcColor, finalAlpha)
                        }
                    }
                }
            } else {
                // 크기가 다른 경우 (기존 로직 유지하되 블렌딩 최적화)
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
        }

        return result
    }

    /**
     * 두 픽셀을 알파 블렌딩 (최적화됨)
     */
    private fun blendPixels(dst: Int, src: Int, srcAlpha: Float): Int {
        if (srcAlpha <= 0f) return dst
        if (srcAlpha >= 1f) return src

        // Fast integer approximation could be used, but for correctness we stick to float for now
        // but optimized to reduce bit shifting overhead if possible.
        // Actually, standard alpha blending: out = src * alpha + dst * (1 - alpha)
        
        val invAlpha = 1f - srcAlpha
        
        val dstA = (dst ushr 24) and 0xFF
        val dstR = (dst ushr 16) and 0xFF
        val dstG = (dst ushr 8) and 0xFF
        val dstB = dst and 0xFF

        val srcR = (src ushr 16) and 0xFF
        val srcG = (src ushr 8) and 0xFF
        val srcB = src and 0xFF

        // Alpha composition
        // outA = srcA + dstA * (1 - srcA)
        // Here srcAlpha is already (srcA * layerOpacity) / 255 normalized to 0..1
        
        // We need to be careful. The srcAlpha passed in is the effective alpha of the source pixel.
        // The dst alpha is 0..255.
        
        val outA = srcAlpha * 255f + dstA * invAlpha
        
        if (outA < 1f) return 0

        val outR = (srcR * srcAlpha + dstR * dstA / 255f * invAlpha) / (outA / 255f)
        val outG = (srcG * srcAlpha + dstG * dstA / 255f * invAlpha) / (outA / 255f)
        val outB = (srcB * srcAlpha + dstB * dstA / 255f * invAlpha) / (outA / 255f)

        return (outA.toInt().coerceIn(0, 255) shl 24) or
               (outR.toInt().coerceIn(0, 255) shl 16) or
               (outG.toInt().coerceIn(0, 255) shl 8) or
               outB.toInt().coerceIn(0, 255)
    }
}
