package io.github.kez.dotkit.tools

import io.github.kez.dotkit.DotKitState
import io.github.kez.dotkit.common.Point
import io.github.kez.dotkit.history.CanvasCommand
import io.github.kez.dotkit.history.DrawPixelsCommand

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

    override fun onDown(state: DotKitState, point: Point, color: Int): ToolState {
        val layer = state.activeLayer ?: return DefaultToolState(point, point, color)

        // 시작 픽셀의 이전 색상 저장
        val affectedPixels = if (layer.isInBounds(point.x, point.y)) {
            val oldColor = layer.getPixel(point.x, point.y)
            intArrayOf(point.x, point.y, oldColor)
        } else {
            IntArray(0)
        }

        return DefaultToolState(
            startPoint = point,
            currentPoint = point,
            color = color,
            affectedPixels = affectedPixels
        )
    }

    override fun onMove(state: DotKitState, point: Point, color: Int, toolState: ToolState?): ToolState? {
        val currentState = toolState as? DefaultToolState ?: return null
        val layer = state.activeLayer ?: return currentState

        // 현재 라인의 픽셀 계산
        val linePixels = ToolUtils.interpolatePixels(currentState.startPoint, point)

        // 새로 영향받는 픽셀의 이전 색상 저장
        val currentAffected = currentState.affectedPixels
        val newAffectedList = IntArray(linePixels.size * 3)
        var count = 0

        for (p in linePixels) {
            if (layer.isInBounds(p.x, p.y)) {
                // Check if already affected
                var exists = false
                var i = 0
                while (i < currentAffected.size) {
                    if (currentAffected[i] == p.x && currentAffected[i + 1] == p.y) {
                        exists = true
                        break
                    }
                    i += 3
                }
                
                if (!exists) {
                    newAffectedList[count++] = p.x
                    newAffectedList[count++] = p.y
                    newAffectedList[count++] = layer.getPixel(p.x, p.y)
                }
            }
        }

        val combined = if (count > 0) {
            val result = IntArray(currentAffected.size + count)
            currentAffected.copyInto(result, 0, 0, currentAffected.size)
            newAffectedList.copyInto(result, currentAffected.size, 0, count)
            result
        } else {
            currentAffected
        }

        return currentState.copy(
            currentPoint = point,
            affectedPixels = combined
        )
    }

    override fun onUp(state: DotKitState, point: Point, color: Int, toolState: ToolState?): CanvasCommand? {
        val finalState = toolState as? DefaultToolState ?: return null
        val layerId = state.activeLayerId ?: return null

        // 최종 직선 픽셀 계산
        val centerPixels = ToolUtils.interpolatePixels(finalState.startPoint, point)

        // 각 중심 픽셀을 선 두께만큼 확장
        val allPixels = centerPixels.flatMap { ToolUtils.expandPoint(it, size) }.distinct()

        // 각 픽셀에 대한 DrawPixelCommand 생성

        if (allPixels.isEmpty()) return null

        // DrawPixelsCommand용 픽셀 데이터 생성
        val pixelData = IntArray(allPixels.size * 3)
        var pixelDataIdx = 0

        for (pixel in allPixels) {
            if (state.activeLayer?.isInBounds(pixel.x, pixel.y) == true) {
                // 현재 픽셀 데이터 추가
                pixelData[pixelDataIdx++] = pixel.x
                pixelData[pixelDataIdx++] = pixel.y
                pixelData[pixelDataIdx++] = color
            }
        }

        // 실제 추가된 픽셀 수에 맞게 배열 크기 조정
        val finalPixelData = if (pixelDataIdx < pixelData.size) {
            pixelData.copyOf(pixelDataIdx)
        } else {
            pixelData
        }

        if (finalPixelData.isEmpty()) return null

        return DrawPixelsCommand(layerId, finalPixelData)
    }

    override fun getPreviewPixels(toolState: ToolState?): List<Pair<Point, Int>> {
        val state = toolState as? DefaultToolState ?: return emptyList()
        val centerPixels = ToolUtils.interpolatePixels(state.startPoint, state.currentPoint)

        // 각 중심 픽셀을 선 두께만큼 확장
        val allPixels = centerPixels.flatMap { ToolUtils.expandPoint(it, size) }.distinct()

        return allPixels.map { it to state.color }
    }
}
