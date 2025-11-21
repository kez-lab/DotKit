package io.github.kez.dotkit.tools

import io.github.kez.dotkit.DotKitState
import io.github.kez.dotkit.common.Point
import io.github.kez.dotkit.history.CanvasCommand
import io.github.kez.dotkit.history.DrawPixelsCommand
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 도형 타입
 */
enum class ShapeType {
    RECTANGLE,  // 사각형
    CIRCLE      // 원
}

/**
 * 채우기 모드
 */
enum class FillMode {
    STROKE,  // 외곽선만
    FILL     // 채우기
}

/**
 * 도형 그리기 도구
 * 사각형과 원을 그립니다.
 *
 * @param size 외곽선 두께 (Stroke 모드에만 적용, 1 = 1픽셀, 2 = 3x3, 3 = 5x5, ...)
 */
class ShapeTool(
    private val shapeType: ShapeType = ShapeType.RECTANGLE,
    private val fillMode: FillMode = FillMode.STROKE,
    private val size: Int = 1
) : Tool {
    override val name: String = when (shapeType) {
        ShapeType.RECTANGLE -> "Rectangle"
        ShapeType.CIRCLE -> "Circle"
    }

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

        // 현재 도형의 픽셀 계산
        val shapePixels = calculateShapePixels(currentState.startPoint, point)

        // 새로 영향받는 픽셀의 이전 색상 저장
        val currentAffected = currentState.affectedPixels
        val newAffectedList = IntArray(shapePixels.size * 3)
        var count = 0

        for (p in shapePixels) {
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

        // 최종 도형 픽셀 계산
        val shapePixels = calculateShapePixels(finalState.startPoint, point)

        // 각 픽셀에 대한 DrawPixelCommand 생성
        val pixels = calculateShapePixels(finalState.startPoint, point)
        
        if (pixels.isEmpty()) return null

        // DrawPixelsCommand용 IntArray 생성 [x, y, color, ...]
        val pixelData = IntArray(pixels.size * 3)
        var idx = 0
        
        for (pixel in pixels) {
            if (state.activeLayer?.isInBounds(pixel.x, pixel.y) == true) {
                pixelData[idx++] = pixel.x
                pixelData[idx++] = pixel.y
                pixelData[idx++] = color
            }
        }
        
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
        val shapePixels = calculateShapePixels(state.startPoint, state.currentPoint)
        return shapePixels.map { it to state.color }
    }

    /**
     * 도형 픽셀 계산
     */
    private fun calculateShapePixels(start: Point, end: Point): List<Point> {
        val centerPixels = when (shapeType) {
            ShapeType.RECTANGLE -> when (fillMode) {
                FillMode.STROKE -> drawRectangleStroke(start, end)
                FillMode.FILL -> drawRectangleFill(start, end)
            }
            ShapeType.CIRCLE -> when (fillMode) {
                FillMode.STROKE -> drawCircleStroke(start, end)
                FillMode.FILL -> drawCircleFill(start, end)
            }
        }

        // Stroke 모드에만 크기 적용
        return if (fillMode == FillMode.STROKE && size > 1) {
            centerPixels.flatMap { expandPoint(it) }.distinct()
        } else {
            centerPixels
        }
    }

    /**
     * 사각형 외곽선 그리기
     */
    private fun drawRectangleStroke(start: Point, end: Point): List<Point> {
        val pixels = mutableSetOf<Point>()
        val x1 = minOf(start.x, end.x)
        val y1 = minOf(start.y, end.y)
        val x2 = maxOf(start.x, end.x)
        val y2 = maxOf(start.y, end.y)

        // 상단 및 하단
        for (x in x1..x2) {
            pixels.add(Point(x, y1))
            pixels.add(Point(x, y2))
        }

        // 좌측 및 우측
        for (y in y1..y2) {
            pixels.add(Point(x1, y))
            pixels.add(Point(x2, y))
        }

        return pixels.toList()
    }

    /**
     * 사각형 채우기
     */
    private fun drawRectangleFill(start: Point, end: Point): List<Point> {
        val pixels = mutableListOf<Point>()
        val x1 = minOf(start.x, end.x)
        val y1 = minOf(start.y, end.y)
        val x2 = maxOf(start.x, end.x)
        val y2 = maxOf(start.y, end.y)

        for (y in y1..y2) {
            for (x in x1..x2) {
                pixels.add(Point(x, y))
            }
        }

        return pixels
    }

    /**
     * 원 외곽선 그리기 (Midpoint Circle Algorithm)
     */
    private fun drawCircleStroke(start: Point, end: Point): List<Point> {
        val centerX = start.x
        val centerY = start.y
        val radius = sqrt(
            ((end.x - start.x).toDouble().pow(2) + (end.y - start.y).toDouble().pow(2))
        ).toInt()

        if (radius == 0) return listOf(start)

        val pixels = mutableSetOf<Point>()
        var x = 0
        var y = radius
        var d = 1 - radius

        // 8방향 대칭 픽셀 추가
        fun addSymmetricPixels(cx: Int, cy: Int, px: Int, py: Int) {
            pixels.add(Point(cx + px, cy + py))
            pixels.add(Point(cx - px, cy + py))
            pixels.add(Point(cx + px, cy - py))
            pixels.add(Point(cx - px, cy - py))
            pixels.add(Point(cx + py, cy + px))
            pixels.add(Point(cx - py, cy + px))
            pixels.add(Point(cx + py, cy - px))
            pixels.add(Point(cx - py, cy - px))
        }

        addSymmetricPixels(centerX, centerY, x, y)

        while (x < y) {
            x++
            if (d < 0) {
                d += 2 * x + 1
            } else {
                y--
                d += 2 * (x - y) + 1
            }
            addSymmetricPixels(centerX, centerY, x, y)
        }

        return pixels.toList()
    }

    /**
     * 원 채우기
     */
    private fun drawCircleFill(start: Point, end: Point): List<Point> {
        val centerX = start.x
        val centerY = start.y
        val radius = sqrt(
            ((end.x - start.x).toDouble().pow(2) + (end.y - start.y).toDouble().pow(2))
        ).toInt()

        if (radius == 0) return listOf(start)

        val pixels = mutableListOf<Point>()
        val radiusSquared = radius * radius

        // 원의 경계 박스 내 모든 픽셀 검사
        for (y in (centerY - radius)..(centerY + radius)) {
            for (x in (centerX - radius)..(centerX + radius)) {
                val dx = x - centerX
                val dy = y - centerY
                if (dx * dx + dy * dy <= radiusSquared) {
                    pixels.add(Point(x, y))
                }
            }
        }

        return pixels
    }

    /**
     * 단일 포인트를 외곽선 두께에 따라 확장
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
