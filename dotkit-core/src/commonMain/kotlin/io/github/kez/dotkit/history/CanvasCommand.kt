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
class DrawPixelsCommand(
    private val layerId: String,
    private val paints: List<PixelPaint> // (x, y, newColor)
) : CanvasCommand {
    data class PixelPaint(val x: Int, val y: Int, val color: Int)
    private var backups: IntArray? = null

    override fun execute(state: DotKitState): DotKitState {
        val layer = state.layerManager.findLayer(layerId) ?: return state
        val w = layer.width; val h = layer.height
        val old = IntArray(paints.size) { i ->
            val p = paints[i]; if (p.x in 0 until w && p.y in 0 until h) layer.getPixel(p.x, p.y) else 0
        }
        backups = old
        return state.updateLayer(layerId) { l ->
            l.copy().also { nl ->
                paints.forEach { p -> if (p.x in 0 until w && p.y in 0 until h) nl.setPixel(p.x, p.y, p.color) }
            }
        }
    }
    override fun undo(state: DotKitState): DotKitState {
        val layer = state.layerManager.findLayer(layerId) ?: return state
        val old = backups ?: return state
        val w = layer.width; val h = layer.height
        return state.updateLayer(layerId) { l ->
            l.copy().also { nl ->
                paints.forEachIndexed { i, p ->
                    if (p.x in 0 until w && p.y in 0 until h) nl.setPixel(p.x, p.y, old[i])
                }
            }
        }
    }
}


/**
 * 라인 그리기 명령 (Bresenham 알고리즘)
 */
data class DrawLineCommand(
    val layerId: String,
    val from: Point,
    val to: Point,
    val color: Int,
    private val affectedPixels: List<Pair<Point, Int>>? = null
) : CanvasCommand {
    override fun execute(state: DotKitState): DotKitState {
        val layer = state.layerManager.findLayer(layerId) ?: return state

        // Bresenham 알고리즘으로 라인상의 모든 픽셀 계산
        val pixels = getLinePixels(from, to)

        // 이전 색상 저장
        val affected = if (affectedPixels == null) {
            pixels.filter { layer.isInBounds(it.x, it.y) }
                .map { point -> point to layer.getPixel(point.x, point.y) }
        } else {
            affectedPixels
        }

        val newState = state.updateLayer(layerId) { layer ->
            layer.copy().also { newLayer ->
                pixels.forEach { point ->
                    if (newLayer.isInBounds(point.x, point.y)) {
                        newLayer.setPixel(point.x, point.y, color)
                    }
                }
            }
        }

        return if (affectedPixels == null) {
            // 첫 실행시 영향받은 픽셀 저장
            copy(affectedPixels = affected)
            newState
        } else {
            newState
        }
    }

    override fun undo(state: DotKitState): DotKitState {
        if (affectedPixels == null) return state

        return state.updateLayer(layerId) { layer ->
            layer.copy().also { newLayer ->
                affectedPixels.forEach { (point, previousColor) ->
                    if (newLayer.isInBounds(point.x, point.y)) {
                        newLayer.setPixel(point.x, point.y, previousColor)
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
