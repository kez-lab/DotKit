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
import io.github.kez.dotkit.canvas.CanvasState
import io.github.kez.dotkit.compose.DotKitCanvas
import io.github.kez.dotkit.compose.ToolAction
import io.github.kez.dotkit.compose.rememberDotKitController
import io.github.kez.dotkit.sample.command.AndroidDroidStamp
import io.github.kez.dotkit.sample.dialogs.*
import io.github.kez.dotkit.tools.*

/**
 * 모바일 최적화 DotKit 샘플 애플리케이션
 *
 * 주요 특징:
 * - 전체 화면 캔버스 (메인 UI)
 * - 다이얼로그 기반 도구/색상/레이어 관리
 * - FloatingActionButton 메뉴로 모든 옵션 접근
 * - 상단 바에 실행 취소/다시 실행/줌 컨트롤
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileApp() {
    var brushSize by remember { mutableStateOf(1) }
    var currentTool by remember(brushSize) { mutableStateOf<Tool>(BrushTool(size = brushSize)) }
    // 다이얼로그 표시 상태
    var showOptions by remember { mutableStateOf(false) }
    var showToolDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showBrushDialog by remember { mutableStateOf(false) }
    var showLayerDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val controller = rememberDotKitController(
        initialState = CanvasState.create(width = 64, height = 80).withZoom(16f)
    )

    LaunchedEffect(Unit) {
        val active = controller.state.activeLayerId ?: return@LaunchedEffect
        controller.execute(
            AndroidDroidStamp.buildCommand(
                originX = 1,
                originY = 10,
                scale = 1,
                layerId = active
            )
        )
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("DotKit")
                            // 현재 색상 표시
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(controller.state.primaryColor))
                                    .border(1.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                            )
                        }
                    },
                    actions = {
                        // 실행 취소
                        IconButton(
                            onClick = { controller.undo() },
                            enabled = controller.canUndo
                        ) {
                            Text("↶")
                        }

                        // 다시 실행
                        IconButton(
                            onClick = { controller.redo() },
                            enabled = controller.canRedo
                        ) {
                            Text("↷")
                        }

                        // 확대
                        IconButton(onClick = { controller.zoomIn() }) {
                            Text("🔍+")
                        }

                        // 축소
                        IconButton(onClick = { controller.zoomOut() }) {
                            Text("🔍-")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showOptions = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("☰", style = MaterialTheme.typography.headlineMedium)
                }
            }
        ) { paddingValues ->
            // 전체 화면 캔버스
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                DotKitCanvas(
                    state = controller.state,
                    activeTool = currentTool,
                    onToolAction = { action ->
                        when (action) {
                            is ToolAction.Execute -> controller.execute(action.command)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 옵션 바텀시트
        if (showOptions) {
            OptionsBottomSheet(
                onDismiss = { showOptions = false },
                onToolsClick = { showToolDialog = true },
                onColorsClick = { showColorDialog = true },
                onBrushClick = { showBrushDialog = true },
                onLayersClick = { showLayerDialog = true },
                onGridToggle = { controller.toggleGrid() },
                onClear = { controller.clear() },
                onSave = { showSaveDialog = true }
            )
        }

        // 도구 선택 다이얼로그
        if (showToolDialog) {
            ToolSelectionDialog(
                currentTool = currentTool,
                brushSize = brushSize,
                onToolSelected = { currentTool = it },
                onDismiss = { showToolDialog = false }
            )
        }

        // 색상 선택 다이얼로그
        if (showColorDialog) {
            ColorPickerDialog(
                currentColor = controller.state.primaryColor,
                onColorSelected = { controller.setPrimaryColor(it) },
                onDismiss = { showColorDialog = false }
            )
        }

        // 브러시 크기 다이얼로그
        if (showBrushDialog) {
            BrushSizeDialog(
                currentSize = brushSize,
                onSizeChanged = { newSize ->
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
                onDismiss = { showBrushDialog = false }
            )
        }

        // 레이어 관리 다이얼로그
        if (showLayerDialog) {
            LayerManagerDialog(
                controller = controller,
                onDismiss = { showLayerDialog = false }
            )
        }

        // 저장 다이얼로그
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("이미지 저장") },
                text = {
                    Text(
                        "이미지 저장 기능은 플랫폼별 구현이 필요합니다.\n" +
                                "Android: MediaStore API\n" +
                                "Desktop: FileDialog\n" +
                                "Web: Canvas download"
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("확인")
                    }
                }
            )
        }
    }
}
