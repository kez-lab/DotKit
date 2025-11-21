package io.github.kez.dotkit.tools

import io.github.kez.dotkit.common.Point
import kotlin.math.abs

/**
 * 도구 관련 유틸리티 함수
 */
object ToolUtils {

    /**
     * Bresenham 알고리즘을 사용하여 두 점 사이의 픽셀 좌표 목록을 반환합니다.
     */
    fun interpolatePixels(p1: Point, p2: Point): List<Point> {
        val pixels = mutableListOf<Point>()

        var x0 = p1.x
        var y0 = p1.y
        val x1 = p2.x
        val y1 = p2.y

        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
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
     * 중심점을 기준으로 주어진 크기만큼 확장된 픽셀 좌표 목록을 반환합니다.
     * size=1: 1x1 (단일 픽셀)
     * size=2: 3x3 (radius 1)
     * size=3: 5x5 (radius 2)
     */
    fun expandPoint(center: Point, size: Int): List<Point> {
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
