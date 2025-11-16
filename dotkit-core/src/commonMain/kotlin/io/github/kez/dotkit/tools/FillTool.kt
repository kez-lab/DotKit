package io.github.kez.dotkit.tools

import io.github.kez.dotkit.DotKitState
import io.github.kez.dotkit.common.Point
import io.github.kez.dotkit.history.CanvasCommand
import io.github.kez.dotkit.history.FillCommand

/**
 * 채우기 도구 (Flood Fill)
 * 클릭한 지점과 같은 색상으로 연결된 모든 영역을 새 색상으로 채웁니다.
 *
 * 4방향 Flood Fill 알고리즘:
 * - Stack 기반 반복 구현 (재귀 오버플로우 방지)
 * - 상하좌우 4방향으로 확산
 * - 연결된 모든 픽셀을 찾아 채움
 */
class FillTool : Tool {
    override val name: String = "Fill"
    override val supportsPreview: Boolean = false

    override fun onDown(state: DotKitState, point: Point, color: Int): ToolState? {
        // Fill은 클릭 시 즉시 실행되므로 툴 상태 불필요
        return null
    }

    override fun onMove(state: DotKitState, point: Point, color: Int, toolState: ToolState?): ToolState? {
        // Fill은 드래그 없음
        return null
    }

    override fun onUp(state: DotKitState, point: Point, color: Int, toolState: ToolState?): CanvasCommand? {
        val layerId = state.activeLayerId ?: return null
        val layer = state.activeLayer ?: return null

        // 경계 체크
        if (!layer.isInBounds(point.x, point.y)) return null

        val targetColor = layer.getPixel(point.x, point.y)

        // 대상 색상과 채울 색상이 같으면 아무것도 하지 않음
        if (targetColor == color) return null

        // Flood Fill 실행
        val affectedPixels = floodFill(layer, point, targetColor)

        // 영향받은 픽셀이 없으면 null 반환
        if (affectedPixels.isEmpty()) return null

        return FillCommand(layerId, affectedPixels, color)
    }

    /**
     * 4방향 Flood Fill 알고리즘
     * Stack 기반으로 재귀 오버플로우 방지
     * 클릭한 지점과 같은 색상으로 연결된 모든 영역을 찾음
     */
    private fun floodFill(
        layer: io.github.kez.dotkit.layers.Layer,
        start: Point,
        targetColor: Int
    ): List<Pair<Point, Int>> {
        val width = layer.width
        val height = layer.height
        val affectedPixels = mutableListOf<Pair<Point, Int>>()
        val visited = Array(height) { BooleanArray(width) }
        val stack = ArrayDeque<Point>()

        stack.addLast(start)

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val x = current.x
            val y = current.y

            // 경계 체크
            if (x < 0 || x >= width || y < 0 || y >= height) continue

            // 이미 방문했으면 스킵
            if (visited[y][x]) continue

            // 색상이 다르면 스킵
            if (layer.getPixel(x, y) != targetColor) continue

            // 현재 픽셀 처리
            visited[y][x] = true
            affectedPixels.add(Point(x, y) to targetColor)

            // 4방향으로 확장 (상하좌우)
            fun tryAddNeighbor(nx: Int, ny: Int) {
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && !visited[ny][nx]) {
                    stack.addLast(Point(nx, ny))
                }
            }

            tryAddNeighbor(x + 1, y)  // 오른쪽
            tryAddNeighbor(x - 1, y)  // 왼쪽
            tryAddNeighbor(x, y + 1)  // 아래
            tryAddNeighbor(x, y - 1)  // 위
        }

        return affectedPixels
    }
}
