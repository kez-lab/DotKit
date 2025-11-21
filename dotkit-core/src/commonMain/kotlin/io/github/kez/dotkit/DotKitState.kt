package io.github.kez.dotkit

import io.github.kez.dotkit.common.Offset
import io.github.kez.dotkit.layers.Layer
import io.github.kez.dotkit.layers.LayerManager

/**
 * DotKit 캔버스의 불변 상태를 나타내는 데이터 클래스
 *
 * @param width 캔버스 너비 (픽셀)
 * @param height 캔버스 높이 (픽셀)
 * @param zoom 줌 레벨 (1.0 = 100%)
 * @param pan 패닝 오프셋
 * @param gridVisible 격자 표시 여부
 * @param gridSnap 격자 스냅 활성화 여부
 * @param gridColor 격자 색상 (ARGB)
 * @param layerManager 레이어 관리자
 * @param activeLayerId 현재 활성 레이어 ID
 * @param primaryColor 기본 색상 (ARGB)
 * @param secondaryColor 보조 색상 (ARGB)
 */
data class DotKitState(
    val width: Int,
    val height: Int,
    val zoom: Float = 1f,
    val pan: Offset = Offset.Companion.ZERO,
    val gridVisible: Boolean = true,
    val gridSnap: Boolean = false,
    val gridColor: Int = 0x4D808080, // 30% 불투명도의 회색
    val layerManager: LayerManager = LayerManager(),
    val activeLayerId: String? = null,
    val primaryColor: Int = 0xFF000000.toInt(), // 검정색
    val secondaryColor: Int = 0xFFFFFFFF.toInt() // 흰색
) {
    init {
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
        require(zoom > 0) { "Zoom must be positive" }
    }

    /**
     * 현재 활성 레이어
     */
    val activeLayer: Layer?
        get() = activeLayerId?.let { layerManager.findLayer(it) }

    /**
     * 모든 레이어 목록
     */
    val layers: List<Layer>
        get() = layerManager.getLayers()

    /**
     * 줌 변경
     */
    fun withZoom(newZoom: Float): DotKitState {
        require(newZoom > 0) { "Zoom must be positive" }
        return copy(zoom = newZoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
    }

    /**
     * 패닝 변경
     */
    fun withPan(newPan: Offset): DotKitState {
        return copy(pan = newPan)
    }

    /**
     * 격자 표시 토글
     */
    fun toggleGrid(): DotKitState {
        return copy(gridVisible = !gridVisible)
    }

    /**
     * 격자 스냅 토글
     */
    fun toggleGridSnap(): DotKitState {
        return copy(gridSnap = !gridSnap)
    }

    /**
     * 레이어 추가
     */
    fun addLayer(layer: Layer): DotKitState {
        return copy(
            layerManager = layerManager.addLayer(layer),
            activeLayerId = layer.id
        )
    }

    /**
     * 레이어 제거
     */
    fun removeLayer(layerId: String): DotKitState {
        val newLayerManager = layerManager.removeLayer(layerId)
        val newActiveLayerId = if (layerId == activeLayerId) {
            // 삭제된 레이어의 인덱스 확인
            val index = layerManager.indexOf(layerId)
            val layers = newLayerManager.getLayers()
            
            if (layers.isEmpty()) {
                null
            } else {
                // 이전 인덱스가 유효하면 그 위치(원래는 다음 레이어였던 것), 아니면 마지막 레이어
                // 또는 바로 위/아래 레이어 선택 로직
                // 여기서는 인덱스를 유지하려고 노력함 (즉, 아래 레이어 선택)
                // index는 삭제 전 인덱스이므로, 삭제 후에는 index 위치에 원래 index+1 레이어가 옴.
                // 만약 index가 마지막이었다면, index-1 (새로운 마지막)을 선택해야 함.
                val newIndex = index.coerceAtMost(layers.lastIndex)
                layers[newIndex].id
            }
        } else {
            activeLayerId
        }
        return copy(
            layerManager = newLayerManager,
            activeLayerId = newActiveLayerId
        )
    }

    /**
     * 활성 레이어 변경
     */
    fun setActiveLayer(layerId: String): DotKitState {
        require(layerManager.findLayer(layerId) != null) {
            "Layer with id $layerId not found"
        }
        return copy(activeLayerId = layerId)
    }

    /**
     * 레이어 업데이트
     */
    fun updateLayer(layerId: String, update: (Layer) -> Layer): DotKitState {
        return copy(layerManager = layerManager.updateLayer(layerId, update))
    }

    /**
     * 레이어 이동
     */
    fun moveLayer(fromIndex: Int, toIndex: Int): DotKitState {
        return copy(layerManager = layerManager.moveLayer(fromIndex, toIndex))
    }

    /**
     * 레이어 복제
     */
    fun duplicateLayer(layerId: String): DotKitState {
        return copy(layerManager = layerManager.duplicateLayer(layerId))
    }

    /**
     * 기본 색상 변경
     */
    fun setPrimaryColor(color: Int): DotKitState {
        return copy(primaryColor = color)
    }

    /**
     * 보조 색상 변경
     */
    fun setSecondaryColor(color: Int): DotKitState {
        return copy(secondaryColor = color)
    }

    /**
     * 기본/보조 색상 교환
     */
    fun swapColors(): DotKitState {
        return copy(
            primaryColor = secondaryColor,
            secondaryColor = primaryColor
        )
    }

    /**
     * 모든 레이어를 합성한 최종 이미지 생성
     */
    fun composite(): IntArray {
        return layerManager.composite(width, height)
    }

    companion object Companion {
        const val MIN_ZOOM = 0.1f
        const val MAX_ZOOM = 32f

        /**
         * 기본 캔버스 상태 생성
         */
        fun create(
            width: Int = 32,
            height: Int = 32
        ): DotKitState {
            val backgroundLayer = Layer.create(
                width = width,
                height = height,
                name = "Background"
            ).apply {
                fill(0xFFFFFFFF.toInt()) // 흰색 배경
            }

            val drawingLayer = Layer.create(
                width = width,
                height = height,
                name = "Layer 1"
            )

            val layerManager = LayerManager()
                .addLayer(backgroundLayer)
                .addLayer(drawingLayer)

            return DotKitState(
                width = width,
                height = height,
                layerManager = layerManager,
                activeLayerId = drawingLayer.id
            )
        }
    }
}