package io.github.kez.dotkit.tools

import io.github.kez.dotkit.canvas.CanvasState
import io.github.kez.dotkit.common.Point
import io.github.kez.dotkit.history.CanvasCommand
import io.github.kez.dotkit.history.CompositeCommand
import io.github.kez.dotkit.history.DrawPixelCommand

/**
 * 브러시 도구
 * 드래그하면 연속적으로 픽셀을 그립니다.
 *
 * @param size 브러시 크기 (1 = 1픽셀, 2 = 3x3, 3 = 5x5, ...)
 */
class BrushTool(
    private val size: Int = 1
) : Tool {
    override val name: String = "Brush"
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

        // 현재 지점부터 새 지점까지의 모든 픽셀 계산 (보간)
        val newPixels = interpolatePixels(currentState.currentPoint, point)

        // 새로 영향받는 픽셀의 이전 색상 저장
        val newAffectedPixels = newPixels
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

        // 시작부터 끝까지 그려진 모든 중심 픽셀에 대한 경로 생성
        val centerPixels = mutableSetOf<Point>()

        // 시작 지점
        centerPixels.add(finalState.startPoint)

        // 중간 지점들 (드래그 경로)
        var lastPoint = finalState.startPoint
        for ((pixel, _) in finalState.affectedPixels) {
            val interpolated = interpolatePixels(lastPoint, pixel)
            centerPixels.addAll(interpolated)
            lastPoint = pixel
        }

        // 최종 지점
        val finalInterpolated = interpolatePixels(lastPoint, point)
        centerPixels.addAll(finalInterpolated)

        // 각 중심 픽셀을 브러시 크기만큼 확장
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

    /**
     * 두 지점 사이의 픽셀을 보간
     * 연속적인 드로잉을 위해 빠진 픽셀을 채웁니다.
     */
    private fun interpolatePixels(from: Point, to: Point): List<Point> {
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
     * 단일 포인트를 브러시 크기에 따라 확장
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

    override fun getPreviewPixels(toolState: ToolState?): List<Pair<Point, Int>> {
        val state = toolState as? DefaultToolState ?: return emptyList()

        // 현재까지 그려진 모든 픽셀 반환 (시작점 + 드래그 경로)
        val centerPixels = mutableListOf<Point>()

        // 시작 픽셀
        centerPixels.add(state.startPoint)

        // 드래그 경로의 모든 픽셀 (보간 포함)
        var lastPoint = state.startPoint
        for ((pixel, _) in state.affectedPixels) {
            val interpolated = interpolatePixels(lastPoint, pixel)
            centerPixels.addAll(interpolated)
            lastPoint = pixel
        }

        // 각 중심 픽셀을 브러시 크기만큼 확장
        val allPixels = centerPixels.distinct()
            .flatMap { expandPoint(it) }
            .distinct()

        // 각 픽셀을 현재 색상과 함께 반환
        return allPixels.map { it to state.color }
    }
}
