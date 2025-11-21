package io.github.kez.dotkit.compose

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import io.github.kez.dotkit.DotKitState
import io.github.kez.dotkit.common.Point
import io.github.kez.dotkit.tools.Tool
import io.github.kez.dotkit.tools.ToolState

/**
 * 픽셀 캔버스 Composable
 *
 * 픽셀 아트 드로잉을 위한 메인 캔버스 UI 컴포넌트입니다.
 * 터치/마우스 입력을 처리하고 캔버스 상태를 렌더링합니다.
 *
 * @param state 캔버스 상태
 * @param activeTool 현재 활성 도구
 * @param onToolAction 도구 액션 콜백 (툴 상태 변화 처리)
 * @param modifier Modifier
 */
@Composable
fun DotKitCanvas(
    state: DotKitState,
    activeTool: Tool,
    onToolAction: (ToolAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var toolState by remember { mutableStateOf<ToolState?>(null) }

    val currentState by rememberUpdatedState(state)
    val currentTool by rememberUpdatedState(activeTool)
    val currentOnToolAction by rememberUpdatedState(onToolAction)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawCanvas(currentState, toolState, currentTool)
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPoint = screenToCanvas(
                        down.position,
                        currentState
                    ) ?: return@awaitEachGesture

                    toolState = currentTool.onDown(
                        state = currentState,
                        point = startPoint,
                        color = currentState.primaryColor
                    )

                    drag(down.id) { change ->
                        val p = screenToCanvas(
                            change.position,
                            currentState
                        )
                        if (p != null) {
                            toolState = currentTool.onMove(
                                state = currentState,
                                point = p,
                                color = currentState.primaryColor,
                                toolState = toolState
                            )
                        }
                        change.consume()
                    }

                    // 3) 업
                    val endPoint = toolState?.currentPoint ?: startPoint
                    val command = currentTool.onUp(
                        state = currentState,
                        point = endPoint,
                        color = currentState.primaryColor,
                        toolState = toolState
                    )
                    if (command != null) {
                        currentOnToolAction(ToolAction.Execute(command))
                    }
                    toolState = null
                }
            }
    )
}


/**
 * 캔버스 렌더링
 */
private fun DrawScope.drawCanvas(
    state: DotKitState,
    toolState: ToolState?,
    activeTool: Tool
) {
    val canvasWidth = state.width * state.zoom
    val canvasHeight = state.height * state.zoom

    drawRect(
        color = Color.White,
        topLeft = Offset(state.pan.x, state.pan.y),
        size = Size(canvasWidth, canvasHeight)
    )

    drawRect(
        color = Color.Black,
        topLeft = Offset(state.pan.x, state.pan.y),
        size = Size(canvasWidth, canvasHeight),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )

    val compositePixels = state.composite()
    val pixelSize = state.zoom

    // Optimize rendering by grouping pixels by color
    val pixelsByColor = mutableMapOf<Int, MutableList<Offset>>()

    for (y in 0 until state.height) {
        for (x in 0 until state.width) {
            val index = y * state.width + x
            if (index in compositePixels.indices) {
                val color = compositePixels[index]
                val alpha = (color shr 24) and 0xFF

                // Skip transparent pixels
                if (alpha == 0) continue

                val screenX = state.pan.x + x * pixelSize + pixelSize / 2 // Center of the pixel
                val screenY = state.pan.y + y * pixelSize + pixelSize / 2 // Center of the pixel

                pixelsByColor.getOrPut(color) { mutableListOf() }.add(Offset(screenX, screenY))
            }
        }
    }

    // Batch draw calls
    pixelsByColor.forEach { (color, points) ->
        drawPoints(
            points = points,
            pointMode = androidx.compose.ui.graphics.PointMode.Points,
            color = Color(color),
            strokeWidth = pixelSize,
            cap = androidx.compose.ui.graphics.StrokeCap.Butt
        )
    }

    if (toolState != null && activeTool.supportsPreview) {
        val previewPixels = activeTool.getPreviewPixels(toolState)
        for ((point, color) in previewPixels) {
            val screenX = state.pan.x + point.x * pixelSize
            val screenY = state.pan.y + point.y * pixelSize

            val previewAlpha = 0.7f
            val originalAlpha = (color shr 24 and 0xFF) / 255f
            val finalAlpha = originalAlpha * previewAlpha

            drawRect(
                color = Color(color).copy(alpha = finalAlpha),
                topLeft = Offset(screenX, screenY),
                size = Size(pixelSize, pixelSize)
            )
        }
    }

    if (state.gridVisible && state.zoom >= 4f) {
        drawGrid(state)
    }
}

/**
 * 격자 그리기
 */
private fun DrawScope.drawGrid(state: DotKitState) {
    val pixelSize = state.zoom
    val gridColor = Color(state.gridColor)

    // 세로선
    for (x in 0..state.width) {
        val screenX = state.pan.x + x * pixelSize
        drawLine(
            color = gridColor,
            start = Offset(screenX, state.pan.y),
            end = Offset(screenX, state.pan.y + state.height * pixelSize),
            strokeWidth = 1f
        )
    }

    // 가로선
    for (y in 0..state.height) {
        val screenY = state.pan.y + y * pixelSize
        drawLine(
            color = gridColor,
            start = Offset(state.pan.x, screenY),
            end = Offset(state.pan.x + state.width * pixelSize, screenY),
            strokeWidth = 1f
        )
    }
}

/**
 * 화면 좌표를 캔버스 좌표로 변환
 */
private fun screenToCanvas(
    screenOffset: Offset,
    state: DotKitState
): Point? {
    val x = ((screenOffset.x - state.pan.x) / state.zoom).toInt()
    val y = ((screenOffset.y - state.pan.y) / state.zoom).toInt()

    // 경계 체크
    return if (x in 0 until state.width && y in 0 until state.height) {
        Point(x, y)
    } else {
        null
    }
}

/**
 * 도구 액션
 */
sealed class ToolAction {
    data class Execute(val command: io.github.kez.dotkit.history.CanvasCommand) : ToolAction()
}
