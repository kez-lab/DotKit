package io.github.kez.dotkit.compose

import androidx.compose.runtime.*
import io.github.kez.dotkit.DotKitState
import io.github.kez.dotkit.common.Offset
import io.github.kez.dotkit.common.Point
import io.github.kez.dotkit.history.CanvasCommand
import io.github.kez.dotkit.history.HistoryManager
import io.github.kez.dotkit.layers.Layer

/**
 * 캔버스 컨트롤러
 *
 * 캔버스 상태를 관리하고 명령형 API를 제공합니다.
 * Composable 함수 내에서 rememberDotKitController()로 생성합니다.
 *
 * @param initialState 초기 캔버스 상태
 * @param maxHistorySize 최대 히스토리 크기 (기본 50)
 */
class DotKitController(
    initialState: DotKitState = DotKitState.create(),
    maxHistorySize: Int = 50
) {
    private val historyManager = HistoryManager(maxHistorySize)

    /**
     * 현재 캔버스 상태
     */
    var state by mutableStateOf(initialState)
        private set

    /**
     * Undo 가능 여부
     */
    val canUndo: Boolean
        get() = historyManager.canUndo

    /**
     * Redo 가능 여부
     */
    val canRedo: Boolean
        get() = historyManager.canRedo

    /**
     * 커맨드 실행
     */
    fun execute(command: CanvasCommand) {
        state = historyManager.execute(state, command)
    }

    /**
     * Undo
     */
    fun undo() {
        state = historyManager.undo(state)
    }

    /**
     * Redo
     */
    fun redo() {
        state = historyManager.redo(state)
    }

    /**
     * 줌 설정
     */
    fun setZoom(zoom: Float) {
        state = state.withZoom(zoom)
    }

    /**
     * 줌 증가
     */
    fun zoomIn(factor: Float = 2f) {
        state = state.withZoom(state.zoom * factor)
    }

    /**
     * 줌 감소
     */
    fun zoomOut(factor: Float = 2f) {
        state = state.withZoom(state.zoom / factor)
    }

    /**
     * 패닝 설정
     */
    fun setPan(pan: Offset) {
        state = state.withPan(pan)
    }

    /**
     * 패닝 이동
     */
    fun panBy(delta: Offset) {
        state = state.withPan(
            Offset(
                x = state.pan.x + delta.x,
                y = state.pan.y + delta.y
            )
        )
    }

    /**
     * 격자 표시 토글
     */
    fun toggleGrid() {
        state = state.toggleGrid()
    }

    /**
     * 격자 스냅 토글
     */
    fun toggleGridSnap() {
        state = state.toggleGridSnap()
    }

    /**
     * 레이어 추가
     */
    fun addLayer(name: String = "New Layer"): String {
        val newLayer = Layer.create(
            width = state.width,
            height = state.height,
            name = name
        )
        state = state.addLayer(newLayer)
        return newLayer.id
    }

    /**
     * 레이어 제거
     */
    fun removeLayer(layerId: String) {
        state = state.removeLayer(layerId)
    }

    /**
     * 활성 레이어 변경
     */
    fun setActiveLayer(layerId: String) {
        state = state.setActiveLayer(layerId)
    }

    /**
     * 레이어 불투명도 설정
     */
    fun setLayerOpacity(layerId: String, opacity: Float) {
        state = state.updateLayer(layerId) { layer ->
            layer.copy(opacity = opacity.coerceIn(0f, 1f))
        }
    }

    /**
     * 레이어 가시성 토글
     */
    fun toggleLayerVisibility(layerId: String) {
        state = state.updateLayer(layerId) { layer ->
            layer.copy(visible = !layer.visible)
        }
    }

    /**
     * 레이어 잠금 토글
     */
    fun toggleLayerLock(layerId: String) {
        state = state.updateLayer(layerId) { layer ->
            layer.copy(locked = !layer.locked)
        }
    }

    /**
     * 레이어 이동
     */
    fun moveLayer(fromIndex: Int, toIndex: Int) {
        state = state.moveLayer(fromIndex, toIndex)
    }

    /**
     * 레이어 복제
     */
    fun duplicateLayer(layerId: String): String {
        state = state.duplicateLayer(layerId)
        // 복제된 레이어 ID 반환 (마지막 레이어)
        return state.layers.lastOrNull()?.id ?: layerId
    }

    /**
     * 기본 색상 설정
     */
    fun setPrimaryColor(color: Int) {
        state = state.setPrimaryColor(color)
    }

    /**
     * 보조 색상 설정
     */
    fun setSecondaryColor(color: Int) {
        state = state.setSecondaryColor(color)
    }

    /**
     * 기본/보조 색상 교환
     */
    fun swapColors() {
        state = state.swapColors()
    }

    /**
     * 캔버스 초기화
     */
    fun clear() {
        state = state.updateLayer(state.activeLayerId ?: return) { layer ->
            layer.apply { clear() }
        }
    }

    /**
     * 캔버스 크기 변경
     */
    fun resize(newWidth: Int, newHeight: Int) {
        // 새 레이어 생성
        val newLayers = state.layers.map { oldLayer ->
            val newLayer = Layer.create(
                width = newWidth,
                height = newHeight,
                name = oldLayer.name
            )

            // 기존 픽셀 복사 (가능한 만큼)
            val minWidth = minOf(oldLayer.width, newWidth)
            val minHeight = minOf(oldLayer.height, newHeight)

            for (y in 0 until minHeight) {
                for (x in 0 until minWidth) {
                    newLayer.setPixel(x, y, oldLayer.getPixel(x, y))
                }
            }

            newLayer.copy(
                opacity = oldLayer.opacity,
                visible = oldLayer.visible,
                locked = oldLayer.locked
            )
        }

        // 새 레이어 매니저 생성
        var newLayerManager = io.github.kez.dotkit.layers.LayerManager()
        for (layer in newLayers) {
            newLayerManager = newLayerManager.addLayer(layer)
        }

        state = state.copy(
            width = newWidth,
            height = newHeight,
            layerManager = newLayerManager,
            activeLayerId = newLayers.find { it.id == state.activeLayerId }?.id
                ?: newLayers.firstOrNull()?.id
        )
    }

    /**
     * 픽셀 그리기 (직접 조작)
     */
    fun drawPixel(x: Int, y: Int, color: Int) {
        val layerId = state.activeLayerId ?: return
        execute(
            io.github.kez.dotkit.history.DrawPixelCommand(
                layerId = layerId,
                x = x,
                y = y,
                color = color,
                previousColor = state.activeLayer?.getPixel(x, y)
            )
        )
    }

    /**
     * 라인 그리기 (직접 조작)
     */
    fun drawLine(from: Point, to: Point, color: Int) {
        val layerId = state.activeLayerId ?: return
        execute(
            io.github.kez.dotkit.history.DrawLineCommand(
                layerId = layerId,
                from = from,
                to = to,
                color = color
            )
        )
    }
}

/**
 * DotKitController를 remember하는 Composable 함수
 */
@Composable
fun rememberDotKitController(
    initialState: DotKitState = DotKitState.create(),
    maxHistorySize: Int = 50
): DotKitController {
    return remember(initialState, maxHistorySize) {
        DotKitController(initialState, maxHistorySize)
    }
}
