package io.github.kez.dotkit.history

import io.github.kez.dotkit.DotKitState
import io.github.kez.dotkit.common.Point
import io.github.kez.dotkit.layers.Layer

/**
 * 캔버스 명령을 나타내는 인터페이스 (커맨드 패턴)
 * 모든 캔버스 수정은 이 인터페이스를 구현하여 실행 취소/다시 실행을 지원합니다.
 */
sealed interface CanvasCommand {
    /**
     * 명령 실행
     * @param state 현재 캔버스 상태
     * @return 명령 실행 후의 새로운 캔버스 상태
     */
    fun execute(state: DotKitState): DotKitState

    /**
     * 명령 취소
     * @param state 현재 캔버스 상태
     * @return 명령 취소 후의 캔버스 상태
     */
    fun undo(state: DotKitState): DotKitState
}

/**
 * 단일 픽셀 그리기 명령
 */
data class DrawPixelCommand(
    val layerId: String,
    val x: Int,
    val y: Int,
    val color: Int,
    private val previousColor: Int? = null
) : CanvasCommand {
    override fun execute(state: DotKitState): DotKitState {
        val layer = state.layerManager.findLayer(layerId) ?: return state

        // 이전 색상 저장 (undo를 위해)
        val prevColor = if (previousColor == null && layer.isInBounds(x, y)) {
            layer.getPixel(x, y)
        } else {
            previousColor
        }

        return state.updateLayer(layerId) { layer ->
            layer.copy().also { it.setPixel(x, y, color) }
        }.let {
            if (previousColor == null) {
                // 첫 실행시 이전 색상 저장
                copy(previousColor = prevColor)
                it
            } else {
                it
            }
        }
    }

    override fun undo(state: DotKitState): DotKitState {
        if (previousColor == null) return state

        return state.updateLayer(layerId) { layer ->
            layer.copy().also { it.setPixel(x, y, previousColor) }
        }
    }
}

/**
 * 다수 픽셀 그리기 명령
 *  배치 픽셀 변경 1건으로 기록
 */
/**
 * 다수 픽셀 그리기 명령 (배치 처리)
 * IntArray 포맷: [x, y, color, x, y, color, ...]
 */
class DrawPixelsCommand(
    private val layerId: String,
    private val pixelData: IntArray // [x, y, color, ...]
) : CanvasCommand {
    // Undo를 위한 백업 데이터: [x, y, oldColor, ...]
    private var backups: IntArray? = null

    override fun execute(state: DotKitState): DotKitState {
        val layer = state.layerManager.findLayer(layerId) ?: return state
        
        // 백업 데이터 생성 (첫 실행 시에만)
        if (backups == null) {
            val newBackups = IntArray(pixelData.size)
            var i = 0
            while (i < pixelData.size) {
                val x = pixelData[i]
                val y = pixelData[i+1]
                // color is at i+2, but we need old color from layer
                
                newBackups[i] = x
                newBackups[i+1] = y
                newBackups[i+2] = if (layer.isInBounds(x, y)) layer.getPixel(x, y) else 0
                
                i += 3
            }
            backups = newBackups
        }

        return state.updateLayer(layerId) { l ->
            l.copy().also { nl ->
                var i = 0
                while (i < pixelData.size) {
                    val x = pixelData[i]
                    val y = pixelData[i+1]
                    val color = pixelData[i+2]
                    
                    if (nl.isInBounds(x, y)) {
                        nl.setPixel(x, y, color)
                    }
                    i += 3
                }
            }
        }
    }

    override fun undo(state: DotKitState): DotKitState {
        val old = backups ?: return state
        
        return state.updateLayer(layerId) { l ->
            l.copy().also { nl ->
                var i = 0
                while (i < old.size) {
                    val x = old[i]
                    val y = old[i+1]
                    val oldColor = old[i+2]
                    
                    if (nl.isInBounds(x, y)) {
                        nl.setPixel(x, y, oldColor)
                    }
                    i += 3
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DrawPixelsCommand

        if (layerId != other.layerId) return false
        if (!pixelData.contentEquals(other.pixelData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = layerId.hashCode()
        result = 31 * result + pixelData.contentHashCode()
        return result
    }
}


/**
 * 라인 그리기 명령 (Bresenham 알고리즘)
 */
data class DrawLineCommand(
    val layerId: String,
    val from: Point,
    val to: Point,
    val color: Int
) : CanvasCommand {
    // [x1, y1, oldColor1, x2, y2, oldColor2, ...]
    private var affectedPixels: IntArray? = null

    override fun execute(state: DotKitState): DotKitState {
        val layer = state.layerManager.findLayer(layerId) ?: return state

        // Bresenham 알고리즘으로 라인상의 모든 픽셀 계산
        val pixels = getLinePixels(from, to)

        // 이전 색상 저장 (첫 실행 시에만)
        if (affectedPixels == null) {
            val validPixels = pixels.filter { layer.isInBounds(it.x, it.y) }
            val arr = IntArray(validPixels.size * 3)
            var idx = 0
            validPixels.forEach { point ->
                arr[idx++] = point.x
                arr[idx++] = point.y
                arr[idx++] = layer.getPixel(point.x, point.y)
            }
            affectedPixels = arr
        }

        return state.updateLayer(layerId) { layer ->
            layer.copy().also { newLayer ->
                pixels.forEach { point ->
                    if (newLayer.isInBounds(point.x, point.y)) {
                        newLayer.setPixel(point.x, point.y, color)
                    }
                }
            }
        }
    }

    override fun undo(state: DotKitState): DotKitState {
        val affected = affectedPixels ?: return state

        return state.updateLayer(layerId) { layer ->
            layer.copy().also { newLayer ->
                var i = 0
                while (i < affected.size) {
                    val x = affected[i++]
                    val y = affected[i++]
                    val oldColor = affected[i++]
                    if (newLayer.isInBounds(x, y)) {
                        newLayer.setPixel(x, y, oldColor)
                    }
                }
            }
        }
    }

    /**
     * Bresenham 알고리즘으로 라인상의 픽셀 계산
     */
    private fun getLinePixels(from: Point, to: Point): List<Point> {
        val pixels = mutableListOf<Point>()

        var x0 = from.x
        var y0 = from.y
        val x1 = to.x
        val y1 = to.y

        val dx = kotlin.math.abs(x1 - x0)
        val dy = kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy

        while (true) {
            pixels.add(Point(x0, y0))

            if (x0 == x1 && y0 == y1) break

            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x0 += sx
            }
            if (e2 < dx) {
                err += dx
                y0 += sy
            }
        }

        return pixels
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DrawLineCommand

        if (layerId != other.layerId) return false
        if (from != other.from) return false
        if (to != other.to) return false
        if (color != other.color) return false
        // affectedPixels is internal state, usually not part of equality for command identity
        // but if we want strict state equality:
        if (affectedPixels != null) {
             if (other.affectedPixels == null) return false
             if (!affectedPixels.contentEquals(other.affectedPixels)) return false
        } else if (other.affectedPixels != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = layerId.hashCode()
        result = 31 * result + from.hashCode()
        result = 31 * result + to.hashCode()
        result = 31 * result + color
        result = 31 * result + (affectedPixels?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * 레이어 추가 명령
 */
data class AddLayerCommand(
    val layer: Layer
) : CanvasCommand {
    override fun execute(state: DotKitState): DotKitState {
        return state.addLayer(layer)
    }

    override fun undo(state: DotKitState): DotKitState {
        return state.removeLayer(layer.id)
    }
}

/**
 * 레이어 제거 명령
 */
data class RemoveLayerCommand(
    val layerId: String,
    private val removedLayer: Layer? = null,
    private val layerIndex: Int? = null
) : CanvasCommand {
    override fun execute(state: DotKitState): DotKitState {
        val layer = state.layerManager.findLayer(layerId) ?: return state
        val index = state.layerManager.indexOf(layerId)

        val newState = state.removeLayer(layerId)

        return if (removedLayer == null) {
            copy(removedLayer = layer, layerIndex = index)
            newState
        } else {
            newState
        }
    }

    override fun undo(state: DotKitState): DotKitState {
        if (removedLayer == null || layerIndex == null) return state

        val newLayerManager = state.layerManager.insertLayer(layerIndex, removedLayer)
        return state.copy(
            layerManager = newLayerManager,
            activeLayerId = removedLayer.id
        )
    }
}

/**
 * 레이어 속성 수정 명령
 */
data class ModifyLayerCommand(
    val layerId: String,
    val modification: (Layer) -> Layer,
    private val originalLayer: Layer? = null
) : CanvasCommand {
    override fun execute(state: DotKitState): DotKitState {
        val layer = state.layerManager.findLayer(layerId) ?: return state

        val original = originalLayer ?: layer
        val newState = state.updateLayer(layerId, modification)

        return if (originalLayer == null) {
            copy(originalLayer = original)
            newState
        } else {
            newState
        }
    }

    override fun undo(state: DotKitState): DotKitState {
        if (originalLayer == null) return state

        return state.updateLayer(layerId) { originalLayer }
    }
}

/**
 * 복합 명령 (여러 명령을 하나로 묶음)
 */
data class CompositeCommand(
    val commands: List<CanvasCommand>
) : CanvasCommand {
    override fun execute(state: DotKitState): DotKitState {
        return commands.fold(state) { currentState, command ->
            command.execute(currentState)
        }
    }

    override fun undo(state: DotKitState): DotKitState {
        return commands.reversed().fold(state) { currentState, command ->
            command.undo(currentState)
        }
    }
}

/**
 * Fill 커맨드 (Flood Fill)
 * 연결된 영역을 채웁니다.
 */
data class FillCommand(
    val layerId: String,
    // [x, y, oldColor, x, y, oldColor, ...]
    val affectedPixels: IntArray, 
    val fillColor: Int
) : CanvasCommand {
    override fun execute(state: DotKitState): DotKitState {
        return state.updateLayer(layerId) { layer ->
            layer.copy().also { newLayer ->
                var i = 0
                while (i < affectedPixels.size) {
                    val x = affectedPixels[i]
                    val y = affectedPixels[i+1]
                    // Skip color (i+2) as we are filling with new color
                    newLayer.setPixel(x, y, fillColor)
                    i += 3
                }
            }
        }
    }

    override fun undo(state: DotKitState): DotKitState {
        return state.updateLayer(layerId) { layer ->
            layer.copy().also { newLayer ->
                var i = 0
                while (i < affectedPixels.size) {
                    val x = affectedPixels[i++]
                    val y = affectedPixels[i++]
                    val previousColor = affectedPixels[i++]
                    newLayer.setPixel(x, y, previousColor)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FillCommand

        if (layerId != other.layerId) return false
        if (!affectedPixels.contentEquals(other.affectedPixels)) return false
        if (fillColor != other.fillColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = layerId.hashCode()
        result = 31 * result + affectedPixels.contentHashCode()
        result = 31 * result + fillColor
        return result
    }
}
