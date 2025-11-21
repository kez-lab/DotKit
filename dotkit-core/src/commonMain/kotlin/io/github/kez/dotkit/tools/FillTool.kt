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
    ): IntArray {
        val width = layer.width
        val height = layer.height
        // 예상 크기: 전체의 1/4 정도로 시작
        var affectedPixels = IntArray(width * height / 4 * 3) 
        var count = 0
        
        val visited = BooleanArray(width * height)
        // Stack using IntArray: [x, y, x, y...]
        val stack = IntArray(width * height * 2)
        var stackTop = 0

        // Push start
        stack[stackTop++] = start.x
        stack[stackTop++] = start.y

        while (stackTop > 0) {
            val y = stack[--stackTop]
            val x = stack[--stackTop]

            // 경계 체크 (이미 넣을 때 체크하지만 안전을 위해)
            if (x < 0 || x >= width || y < 0 || y >= height) continue

            val index = y * width + x
            // 이미 방문했으면 스킵
            if (visited[index]) continue

            // 색상이 다르면 스킵
            if (layer.getPixel(x, y) != targetColor) continue

            // 현재 픽셀 처리
            visited[index] = true
            
            // Ensure capacity
            if (count + 3 > affectedPixels.size) {
                val newSize = affectedPixels.size * 2
                affectedPixels = affectedPixels.copyOf(newSize)
            }
            
            affectedPixels[count++] = x
            affectedPixels[count++] = y
            affectedPixels[count++] = targetColor

            // 4방향으로 확장 (상하좌우)
            // Push neighbors
            if (x + 1 < width && !visited[y * width + (x + 1)]) {
                stack[stackTop++] = x + 1
                stack[stackTop++] = y
            }
            if (x - 1 >= 0 && !visited[y * width + (x - 1)]) {
                stack[stackTop++] = x - 1
                stack[stackTop++] = y
            }
            if (y + 1 < height && !visited[(y + 1) * width + x]) {
                stack[stackTop++] = x
                stack[stackTop++] = y + 1
            }
            if (y - 1 >= 0 && !visited[(y - 1) * width + x]) {
                stack[stackTop++] = x
                stack[stackTop++] = y - 1
            }
        }

        return affectedPixels.copyOf(count)
    }
}
