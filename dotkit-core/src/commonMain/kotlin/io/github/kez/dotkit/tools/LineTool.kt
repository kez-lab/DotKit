package io.github.kez.dotkit.tools

import io.github.kez.dotkit.canvas.CanvasState
import io.github.kez.dotkit.common.Point
import io.github.kez.dotkit.history.CanvasCommand
import io.github.kez.dotkit.history.CompositeCommand
import io.github.kez.dotkit.history.DrawPixelCommand

/**
 * 직선 도구
 * Bresenham 알고리즘을 사용하여 픽셀 완벽한 직선을 그립니다.
 *
 * @param size 선 두께 (1 = 1픽셀, 2 = 3x3, 3 = 5x5, ...)
 */
class LineTool(
    private val size: Int = 1
) : Tool {
    override val name: String = "Line"
    override val supportsPreview: Boolean = true

    override fun onDown(state: CanvasState, point: Point, color: Int): ToolState {
        val layer = state.activeLayer ?: return DefaultToolState(point, point, color)

        // 시작 픽셀의 이전 색상 저장
        val affectedPixels = if (layer.isInBounds(point.x, point.y)) {
            listOf(point to layer.getPixel(point.x, point.y))
        } else {
            emptyList()
        }

        return DefaultToolState(
            startPoint = point,
            currentPoint = point,
            color = color,
            affectedPixels = affectedPixels
        )
    }

    override fun onMove(state: CanvasState, point: Point, color: Int, toolState: ToolState?): ToolState? {
        val currentState = toolState as? DefaultToolState ?: return null
        val layer = state.activeLayer ?: return currentState

        // 현재 라인의 픽셀 계산
        val linePixels = bresenhamLine(currentState.startPoint, point)

        // 새로 영향받는 픽셀의 이전 색상 저장 (중복 제외)
        val newAffectedPixels = linePixels
            .filter { layer.isInBounds(it.x, it.y) }
            .filter { p -> currentState.affectedPixels.none { it.first == p } }
            .map { p -> p to layer.getPixel(p.x, p.y) }

        return currentState.copy(
            currentPoint = point,
            affectedPixels = currentState.affectedPixels + newAffectedPixels
        )
    }

    override fun onUp(state: CanvasState, point: Point, color: Int, toolState: ToolState?): CanvasCommand? {
        val finalState = toolState as? DefaultToolState ?: return null
        val layerId = state.activeLayerId ?: return null

        // 최종 직선 픽셀 계산
        val centerPixels = bresenhamLine(finalState.startPoint, point)

        // 각 중심 픽셀을 선 두께만큼 확장
        val allPixels = centerPixels.flatMap { expandPoint(it) }.distinct()

        // 각 픽셀에 대한 DrawPixelCommand 생성
        val commands = allPixels.mapNotNull { pixel ->
            val previousColor = finalState.affectedPixels.find { it.first == pixel }?.second
                ?: state.activeLayer?.getPixel(pixel.x, pixel.y)
            if (state.activeLayer?.isInBounds(pixel.x, pixel.y) == true) {
                DrawPixelCommand(layerId, pixel.x, pixel.y, color, previousColor)
            } else {
                null
            }
        }

        return if (commands.isEmpty()) null else CompositeCommand(commands)
    }

    override fun getPreviewPixels(toolState: ToolState?): List<Pair<Point, Int>> {
        val state = toolState as? DefaultToolState ?: return emptyList()
        val centerPixels = bresenhamLine(state.startPoint, state.currentPoint)

        // 각 중심 픽셀을 선 두께만큼 확장
        val allPixels = centerPixels.flatMap { expandPoint(it) }.distinct()

        return allPixels.map { it to state.color }
    }

    /**
     * Bresenham의 직선 알고리즘
     * 두 점 사이의 픽셀 완벽한 직선을 계산합니다.
     *
     * 특징:
     * - 정수 연산만 사용 (부동소수점 연산 없음)
     * - 빠르고 효율적
     * - 픽셀 완벽한 직선 생성
     */
    private fun bresenhamLine(from: Point, to: Point): List<Point> {
        if (from == to) return listOf(from)

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

    /**
     * 단일 포인트를 선 두께에 따라 확장
     * size=1: 1x1 (단일 픽셀)
     * size=2: 3x3 (radius 1)
     * size=3: 5x5 (radius 2)
     */
    private fun expandPoint(center: Point): List<Point> {
        if (size <= 1) return listOf(center)

        val radius = size - 1
        val pixels = mutableListOf<Point>()

        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                pixels.add(Point(center.x + dx, center.y + dy))
            }
        }

        return pixels
    }
}
