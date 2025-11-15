package io.github.kez.dotkit.sample.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kez.dotkit.tools.*

/**
 * 도구 선택 다이얼로그
 */
@Composable
fun ToolSelectionDialog(
    currentTool: Tool,
    brushSize: Int = 1,
    onToolSelected: (Tool) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎨 도구 선택") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tools = listOf(
                    BrushTool(size = brushSize) to "✏️ 브러시",
                    LineTool(size = brushSize) to "📏 라인",
                    ShapeTool(ShapeType.RECTANGLE, FillMode.STROKE, size = brushSize) to "⬜ 사각형",
                    ShapeTool(ShapeType.CIRCLE, FillMode.STROKE, size = brushSize) to "⭕ 원",
                    EraserTool(size = brushSize) to "🧹 지우개",
                    EyedropperTool() to "💧 스포이드"
                )

                tools.forEach { (tool, name) ->
                    Button(
                        onClick = {
                            onToolSelected(tool)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (currentTool::class == tool::class) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        Text(name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}
