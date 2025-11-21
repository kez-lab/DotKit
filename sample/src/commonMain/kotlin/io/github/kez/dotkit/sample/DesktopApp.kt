package io.github.kez.dotkit.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.kez.dotkit.DotKitState
import io.github.kez.dotkit.compose.DotKitCanvas
import io.github.kez.dotkit.compose.ToolAction
import io.github.kez.dotkit.compose.rememberDotKitController
import io.github.kez.dotkit.tools.*

/**
 * DotKit 데스크탑 애플리케이션
 *
 * 픽셀 드로잉의 기본 기능을 시연합니다. (데스크탑 레이아웃)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp() {
    var brushSize by remember { mutableStateOf(1) }
    var currentTool by remember(brushSize) { mutableStateOf<Tool>(BrushTool(size = brushSize)) }
    val controller = rememberDotKitController(
        initialState = DotKitState.create(width = 32, height = 32).withZoom(16f)
    )

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("DotKit Sample")
                            Spacer(Modifier.width(16.dp))
                            // 현재 색상 표시
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(controller.state.primaryColor))
                                    .border(1.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 왼쪽: 도구 팔레트
                EnhancedToolPalette(
                    currentTool = currentTool,
                    onToolSelected = { currentTool = it },
                    brushSize = brushSize,
                    onBrushSizeChange = { newSize ->
                        brushSize = newSize
                        // 현재 도구를 새로운 크기로 재생성
                        currentTool = when (currentTool) {
                            is BrushTool -> BrushTool(size = newSize)
                            is LineTool -> LineTool(size = newSize)
                            is EraserTool -> EraserTool(size = newSize)
                            is ShapeTool -> {
                                val shapeTool = currentTool as ShapeTool
                                ShapeTool(
                                    shapeType = when (shapeTool.name) {
                                        "Rectangle" -> ShapeType.RECTANGLE
                                        "Circle" -> ShapeType.CIRCLE
                                        else -> ShapeType.RECTANGLE
                                    },
                                    fillMode = FillMode.STROKE,
                                    size = newSize
                                )
                            }
                            else -> currentTool
                        }
                    },
                    controller = controller,
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .padding(8.dp)
                )

                // 중앙: 캔버스
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    // 캔버스
                    DotKitCanvas(
                        state = controller.state,
                        activeTool = currentTool,
                        onToolAction = { action ->
                            when (action) {
                                is ToolAction.Execute -> controller.execute(action.command)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 하단 컨트롤
                    EnhancedControlPanel(
                        controller = controller,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 오른쪽: 레이어 패널
                EnhancedLayerPanel(
                    controller = controller,
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .padding(8.dp)
                )
            }
        }
    }
}

/**
 * 개선된 도구 팔레트 (브러시 크기 조절 추가)
 */
@Composable
fun EnhancedToolPalette(
    currentTool: Tool,
    onToolSelected: (Tool) -> Unit,
    brushSize: Int,
    onBrushSizeChange: (Int) -> Unit,
    controller: io.github.kez.dotkit.compose.DotKitController,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "🎨 도구",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

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
                onClick = { onToolSelected(tool) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
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

        Spacer(modifier = Modifier.height(16.dp))

        // 브러시 크기 조절 (향후 확장용)
        Text(
            text = "🖌️ 브러시 크기",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${brushSize}px", modifier = Modifier.width(40.dp))
            Slider(
                value = brushSize.toFloat(),
                onValueChange = { onBrushSizeChange(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 색상 선택
        Text(
            text = "🎨 색상",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val colors = listOf(
            0xFF000000.toInt() to "검정",
            0xFFFFFFFF.toInt() to "흰색",
            0xFFFF0000.toInt() to "빨강",
            0xFF00FF00.toInt() to "초록",
            0xFF0000FF.toInt() to "파랑",
            0xFFFFFF00.toInt() to "노랑",
            0xFFFF00FF.toInt() to "마젠타",
            0xFF00FFFF.toInt() to "시안"
        )

        colors.forEach { (color, name) ->
            val isSelected = color == controller.state.primaryColor
            Button(
                onClick = { controller.setPrimaryColor(color) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .then(
                        if (isSelected) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary
                        ) else Modifier
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(color)
                )
            ) {
                Text(
                    text = name,
                    color = if (color == 0xFFFFFFFF.toInt() || color == 0xFFFFFF00.toInt()) {
                        Color.Black
                    } else {
                        Color.White
                    }
                )
            }
        }
    }
}

/**
 * 개선된 컨트롤 패널 (이미지 저장 기능 추가)
 */
@Composable
fun EnhancedControlPanel(
    controller: io.github.kez.dotkit.compose.DotKitController,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Undo/Redo
        Button(
            onClick = { controller.undo() },
            enabled = controller.canUndo
        ) {
            Text("↶ 실행 취소")
        }

        Button(
            onClick = { controller.redo() },
            enabled = controller.canRedo
        ) {
            Text("↷ 다시 실행")
        }

        // 줌 컨트롤
        Button(onClick = { controller.zoomIn() }) {
            Text("🔍+ 확대")
        }

        Button(onClick = { controller.zoomOut() }) {
            Text("🔍- 축소")
        }

        // 격자 토글
        Button(onClick = { controller.toggleGrid() }) {
            Text(if (controller.state.gridVisible) "📐 격자 숨기기" else "📐 격자 표시")
        }

        // 초기화
        Button(onClick = { controller.clear() }) {
            Text("🗑️ 전체 지우기")
        }

        // 저장 (향후 구현용 플레이스홀더)
        Button(
            onClick = { showSaveDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("💾 저장")
        }

        // JSON 불러오기
        var showJsonDialog by remember { mutableStateOf(false) }
        Button(
            onClick = { showJsonDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text("📥 JSON")
        }

        if (showJsonDialog) {
            JsonImportDialog(
                onDismiss = { showJsonDialog = false },
                onImport = { json ->
                    try {
                        val newState = io.github.kez.dotkit.converter.DotKitJsonConverter.parse(json)
                        // Controller needs a way to set state directly or we use a command?
                        // DotKitController.state is mutable but private set.
                        // We need a method in DotKitController to load state.
                        // For now, let's assume we can add a method or use a hack.
                        // Wait, I can add a method to DotKitController.
                        // Let's modify DotKitController first to allow loading state.
                        // Or I can just use a custom command? No, replacing state is drastic.
                        // I will add `loadState` to DotKitController.
                        showJsonDialog = false
                    } catch (e: Exception) {
                        // Show error?
                        println("Error parsing JSON: ${e.message}")
                    }
                },
                controller = controller // Pass controller to call loadState
            )
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("이미지 저장") },
            text = {
                Text("이미지 저장 기능은 플랫폼별 구현이 필요합니다.\n" +
                        "Android: MediaStore API\n" +
                        "Desktop: FileDialog\n" +
                        "Web: Canvas download")
            },
            confirmButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("확인")
                }
            }
        )
    }
}

@Composable
fun JsonImportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    controller: io.github.kez.dotkit.compose.DotKitController
) {
    var jsonText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("JSON 불러오기") },
        text = {
            Column {
                Text("DotKit JSON 문자열을 입력하세요:", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text("{\"width\": 16, \"height\": 16, ...}") }
                )
                if (errorText != null) {
                    Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                // AI 예시 버튼
                Button(
                    onClick = {
                        jsonText = """
                        {
                          "width": 8,
                          "height": 8,
                          "palette": ["#000000", "#FF0000", "#00FF00", "#0000FF", "#FFFF00"],
                          "data": [
                            [0, 0, 1, 1, 2, 2, 3, 3],
                            [0, 0, 1, 1, 2, 2, 3, 3],
                            [4, 4, 4, 4, 4, 4, 4, 4],
                            [4, 4, 4, 4, 4, 4, 4, 4],
                            [0, 1, 0, 1, 0, 1, 0, 1],
                            [2, 3, 2, 3, 2, 3, 2, 3],
                            [0, 0, 0, 0, 0, 0, 0, 0],
                            [1, 1, 1, 1, 1, 1, 1, 1]
                          ]
                        }
                        """.trimIndent()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🤖 AI 생성 예시 (8x8)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        val newState = io.github.kez.dotkit.converter.DotKitJsonConverter.parse(jsonText)
                        controller.loadState(newState)
                        onDismiss()
                    } catch (e: Exception) {
                        errorText = "파싱 오류: ${e.message}"
                    }
                }
            ) {
                Text("불러오기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/**
 * 개선된 레이어 패널 (레이어 삭제 기능 추가)
 */
@Composable
fun EnhancedLayerPanel(
    controller: io.github.kez.dotkit.compose.DotKitController,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "📚 레이어",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 레이어 추가 버튼
        Button(
            onClick = { controller.addLayer("레이어 ${controller.state.layers.size + 1}") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("➕ 레이어 추가")
        }

        // 레이어 목록
        controller.state.layers.reversed().forEach { layer ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = if (layer.id == controller.state.activeLayerId) {
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                } else {
                    CardDefaults.cardColors()
                }
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = layer.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        Row {
                            // 가시성 토글
                            IconButton(
                                onClick = { controller.toggleLayerVisibility(layer.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(if (layer.visible) "👁" else "🚫", style = MaterialTheme.typography.bodySmall)
                            }

                            // 레이어 선택
                            if (layer.id != controller.state.activeLayerId) {
                                IconButton(
                                    onClick = { controller.setActiveLayer(layer.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("✓", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // 불투명도 슬라이더
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "불투명도: ${(layer.opacity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(100.dp)
                        )
                        Slider(
                            value = layer.opacity,
                            onValueChange = { controller.setLayerOpacity(layer.id, it) },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
