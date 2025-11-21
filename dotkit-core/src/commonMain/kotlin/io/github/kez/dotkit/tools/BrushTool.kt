package io.github.kez.dotkit.tools

import io.github.kez.dotkit.DotKitState
import io.github.kez.dotkit.common.Point
import io.github.kez.dotkit.history.CanvasCommand
import io.github.kez.dotkit.history.DrawPixelsCommand

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

        // 현재 지점부터 새 지점까지의 모든 픽셀 계산 (보간)
        val newPixels = ToolUtils.interpolatePixels(currentState.currentPoint, point)

        // 새로 영향받는 픽셀의 이전 색상 저장
        // 기존 affectedPixels에 없는 픽셀만 추가
        val currentAffected = currentState.affectedPixels
        val newAffectedList = IntArray(newPixels.size * 3)
        var count = 0

        for (p in newPixels) {
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

        // 재구성을 위해 affectedPixels를 순회하며 확장
        val allPixels = mutableSetOf<Point>()
        
        var i = 0
        while (i < finalState.affectedPixels.size) {
            val x = finalState.affectedPixels[i++]
            val y = finalState.affectedPixels[i++]
            i++ // skip color
            
            val center = Point(x, y)
            allPixels.addAll(ToolUtils.expandPoint(center, size))
        }
        
        // 마지막 구간 (currentPoint -> point) 처리
        val finalInterpolated = ToolUtils.interpolatePixels(finalState.currentPoint, point)
        for (p in finalInterpolated) {
             allPixels.addAll(ToolUtils.expandPoint(p, size))
        }

        if (allPixels.isEmpty()) return null

        // 2. DrawPixelsCommand용 IntArray 생성 [x, y, color, ...]
        val pixelData = IntArray(allPixels.size * 3)
        var idx = 0
        
        for (pixel in allPixels) {
            if (state.activeLayer?.isInBounds(pixel.x, pixel.y) == true) {
                pixelData[idx++] = pixel.x
                pixelData[idx++] = pixel.y
                pixelData[idx++] = color
            }
        }
        
        // 유효한 픽셀만 포함된 배열로 자르기 (if any were out of bounds)
        val finalPixelData = if (idx < pixelData.size) {
            pixelData.copyOf(idx)
        } else {
            pixelData
        }

        if (finalPixelData.isEmpty()) return null

        return DrawPixelsCommand(layerId, finalPixelData)
    }

    override fun getPreviewPixels(toolState: ToolState?): List<Pair<Point, Int>> {
        val state = toolState as? DefaultToolState ?: return emptyList()

        val allPixels = mutableSetOf<Point>()
        
        var i = 0
        while (i < state.affectedPixels.size) {
            val x = state.affectedPixels[i++]
            val y = state.affectedPixels[i++]
            i++ // skip color
            
            val center = Point(x, y)
            allPixels.addAll(ToolUtils.expandPoint(center, size))
        }

        return allPixels.map { it to state.color }
    }
}
