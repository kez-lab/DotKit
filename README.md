
<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin" alt="Kotlin Multiplatform"/>
  <img src="https://img.shields.io/badge/Compose-Multiplatform-green?logo=jetpack-compose" alt="Compose Multiplatform"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="MIT License"/>
</p>

Compose Multiplatform UI를 지원하는 Kotlin Multiplatform 기반 픽셀 아트 드로잉 라이브러리입니다. 
Android, Desktop(JVM), iOS, Web(Wasm)에서 픽셀 캔버스 작업을 위한 통합 API를 제공합니다.

## Screenshot

| iOS | Desktop | Web |
|:--:|:--:|:--:|
| <img width="250" src="https://github.com/user-attachments/assets/b062b6ee-4566-4a00-ac52-8ec83bf6645d" /> | <img width="250" src="https://github.com/user-attachments/assets/8c2ce194-07fa-4a59-8af6-2ca1cc670e9b" /> | <img width="250" src="https://github.com/user-attachments/assets/c59be363-b173-432a-aece-473da794b07e" /> |


| SampleScreenshot | Demo Video |
|:-------------:|:-------------:|
| <img width="240" src="https://github.com/user-attachments/assets/917d2808-59bf-4a2d-8dcb-27f560d31a7d" /> | <img width="240" src="https://github.com/user-attachments/assets/44e08810-949e-4256-b2aa-d9de25fc88aa"/>|  |


## Features

### Core Drawing Tools
- **BrushTool**: 브러시 크기(1–5 픽셀) 조절 가능한 자유곡선 드로잉
- **LineTool**: Bresenham 알고리즘 기반의 픽셀 정밀 직선
- **ShapeTool**: 사각형/원 도형(채우기/스트로크 모드)
- **EraserTool**: 투명 픽셀로 지우기(사이즈 조절)
- **EyedropperTool**: 캔버스 픽셀에서 색상 스포이드

### Layer System
- 다중 레이어 및 z-order 관리
- 레이어별 불투명도(0.0–1.0)
- 레이어 보이기/잠금 상태
- 레이어 재정렬/복제
- 알파 블렌딩 합성

### State Management
- 커맨드 패턴 기반 전체 Undo/Redo(최대 50 스텝)
- 줌/팬 뷰포트 컨트롤(0.1x–32x)
- 줌 4x 이상에서 자동 표시되는 그리드

### Image Export
- 플랫폼별 PNG 내보내기 구현
- Web은 Data URL(Base64) 제공
- Native 최적화를 위한 expect/actual 패턴

## Installation
**Maven 배포 예정**

```

### Platform Configuration

**Android**
```kotlin
android {
    namespace = "com.example.app"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
}
```

**Desktop (JVM)**
```kotlin
jvm("desktop")
```

**iOS**
```kotlin
iosX64()
iosArm64()
iosSimulatorArm64()
```

**Web (Wasm)**
```kotlin
@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
wasmJs { browser() }
```

## Quick Start

### Basic Setup

```kotlin
import androidx.compose.runtime.*
import io.github.kez.dotkit.DotKitState
import io.github.kez.dotkit.compose.*
import io.github.kez.dotkit.tools.*

@Composable
fun PixelEditor() {
    var brushSize by remember { mutableStateOf(1) }
    var currentTool by remember(brushSize) {
        mutableStateOf<Tool>(BrushTool(size = brushSize))
    }

    val controller = rememberDotKitController(
        initialState = DotKitState.create(width = 64, height = 64)
    )

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
```

### Tool Creation

```kotlin
// 크기 지정 브러시
val brush = BrushTool(size = 3)

// 두께 있는 직선
val line = LineTool(size = 2)

// 스트로크 모드 사각형
val rect = ShapeTool(
    shapeType = ShapeType.RECTANGLE,
    fillMode = FillMode.STROKE,
    size = 1
)

// 채우기 모드 원
val circle = ShapeTool(
    shapeType = ShapeType.CIRCLE,
    fillMode = FillMode.FILL
)

// 지우개
val eraser = EraserTool(size = 2)

// 스포이드
val eyedropper = EyedropperTool()
```

## API Reference

### dotkit-core Module

플랫폼 독립 드로잉 엔진이 포함된 코어 비즈니스 로직 모듈

**Dependencies**: `kotlinx-coroutines-core` only

#### DotKitState

불변 캔버스 상태 관리.

```kotlin
data class DotKitState(
    val width: Int,
    val height: Int,
    val zoom: Float = 1f,
    val pan: Offset = Offset.ZERO,
    val gridVisible: Boolean = true,
    val layerManager: LayerManager,
    val primaryColor: Int,
    val secondaryColor: Int
)

// Factory
DotKitState.create(width: Int = 32, height: Int = 32): DotKitState

// 변환
fun withZoom(newZoom: Float): DotKitState
fun withPan(newPan: Offset): DotKitState
fun toggleGrid(): DotKitState
fun addLayer(layer: Layer): DotKitState
fun removeLayer(layerId: String): DotKitState
fun setActiveLayer(layerId: String): DotKitState
fun updateLayer(layerId: String, update: (Layer) -> Layer): DotKitState
fun composite(): IntArray  // 최종 합성 픽셀
```

#### Layer

픽셀 버퍼 + 불투명도/가시성 제어.

```kotlin
data class Layer(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val opacity: Float = 1f,
    val visible: Boolean = true,
    val locked: Boolean = false
)

// Factory
Layer.create(width: Int, height: Int, name: String): Layer

// Pixel ops
fun getPixel(x: Int, y: Int): Int
fun setPixel(x: Int, y: Int, color: Int)
fun fill(color: Int)
fun clear()  // 투명 채우기
fun isInBounds(x: Int, y: Int): Boolean
```

#### Tool Interface

모든 도구의 기본 인터페이스.

```kotlin
interface Tool {
    val name: String
    val supportsPreview: Boolean

    fun onDown(state: DotKitState, point: Point, color: Int): ToolState?
    fun onMove(state: DotKitState, point: Point, color: Int, toolState: ToolState?): ToolState?
    fun onUp(state: DotKitState, point: Point, color: Int, toolState: ToolState?): CanvasCommand?
    fun getPreviewPixels(toolState: ToolState?): List<Pair<Point, Int>>
}

interface ToolState {
    val startPoint: Point
    val currentPoint: Point
    val color: Int
    val affectedPixels: List<Pair<Point, Int>>
}
```

#### HistoryManager

커맨드 패턴 기반 Undo/Redo 시스템.

```kotlin
class HistoryManager(maxHistorySize: Int = 50) {
    val canUndo: Boolean
    val canRedo: Boolean

    fun execute(state: DotKitState, command: CanvasCommand): DotKitState
    fun undo(state: DotKitState): DotKitState
    fun redo(state: DotKitState): DotKitState
    fun clear()
}

interface CanvasCommand {
    fun execute(state: DotKitState): DotKitState
    fun undo(state: DotKitState): DotKitState
}

// 내장 커맨드
class DrawPixelCommand(layerId: String, x: Int, y: Int, color: Int, previousColor: Int)
class DrawLineCommand(layerId: String, from: Point, to: Point, color: Int)
class CompositeCommand(commands: List<CanvasCommand>)
```

#### ImageExporter (expect/actual)

플랫폼별 이미지 내보내기.

```kotlin
expect class ImageExporter {
    fun exportPNG(pixels: IntArray, width: Int, height: Int): ByteArray
}

// 구현
// - android: android.graphics.Bitmap
// - desktop: java.awt.image.BufferedImage
// - ios: UIKit.UIImage
// - wasm: HTML5 Canvas API
```

### dotkit-compose Module

Compose Multiplatform UI 통합 모듈

**Dependencies**
- `compose.runtime`, `compose.foundation`, `compose.material3`, `compose.ui`
- `dotkit-core`

#### DotKitController

명령형 API로 캔버스를 조작하는 컨트롤러.

```kotlin
class DotKitController(
    initialState: DotKitState = DotKitState.create(),
    maxHistorySize: Int = 50
) {
    var state: DotKitState
    val canUndo: Boolean
    val canRedo: Boolean

    // 명령 실행
    fun execute(command: CanvasCommand)
    fun undo()
    fun redo()

    // 뷰포트
    fun setZoom(zoom: Float)
    fun zoomIn(factor: Float = 2f)
    fun zoomOut(factor: Float = 2f)
    fun setPan(pan: Offset)
    fun panBy(delta: Offset)

    // 그리드
    fun toggleGrid()
    fun toggleGridSnap()

    // 레이어
    fun addLayer(name: String = "New Layer"): String
    fun removeLayer(layerId: String)
    fun setActiveLayer(layerId: String)
    fun setLayerOpacity(layerId: String, opacity: Float)
    fun toggleLayerVisibility(layerId: String)
    fun toggleLayerLock(layerId: String)
    fun moveLayer(fromIndex: Int, toIndex: Int)
    fun duplicateLayer(layerId: String): String

    // 색상
    fun setPrimaryColor(color: Int)
    fun setSecondaryColor(color: Int)
    fun swapColors()

    // 직접 그리기
    fun drawPixel(x: Int, y: Int, color: Int)
    fun drawLine(from: Point, to: Point, color: Int)

    // 유틸
    fun clear()
    fun resize(newWidth: Int, newHeight: Int)
}

@Composable
fun rememberDotKitController(
    initialState: DotKitState = DotKitState.create(),
    maxHistorySize: Int = 50
): DotKitController
```

#### DotKitCanvas

메인 캔버스 컴포저블(렌더링 + 입력 처리).

```kotlin
@Composable
fun DotKitCanvas(
    state: DotKitState,
    activeTool: Tool,
    onToolAction: (ToolAction) -> Unit,
    modifier: Modifier = Modifier
)

sealed class ToolAction {
    data class Execute(val command: CanvasCommand) : ToolAction()
}
```

**렌더링 파이프라인**
1. `state.composite()`로 레이어 합성
2. `drawRect`로 픽셀 렌더링
3. `tool.supportsPreview == true`면 프리뷰 오버레이
4. `state.gridVisible && state.zoom >= 4f`일 때 그리드 오버레이

**제스처 처리**
- `pointerInput(Unit)` + `awaitEachGesture`/`drag()` 사용
- `rememberUpdatedState`로 안전한 캡처
- `change.consume()`로 이벤트 전파 방지

#### ZoomPanHandler (expect/actual)

플랫폼별 줌/팬 제스처.

```kotlin
expect class ZoomPanHandler {
    fun handleZoom(...)
    fun handlePan(...)
}
```

- android: ScaleGestureDetector/GestureDetector
- desktop: 마우스 휠 + 드래그
- ios: UIPinch/UIPan
- wasm: Pointer/Wheel 이벤트

## Architecture

### Design Patterns

**Immutable State Pattern**
```
모든 상태 변환은 copy()로 새로운 인스턴스 반환
- 예측 가능한 상태 전이
- Undo/Redo 구현 용이
- 기본적으로 스레드 안전
```

**Command Pattern**
```
모든 작업은 CanvasCommand
- execute(): 적용
- undo(): 되돌리기
- 복합 작업 조합 용이
```

**Strategy Pattern (Tools)**
```
Tool 인터페이스로 런타임 도구 전환
- 독립 구현
- 손쉬운 확장
- 무상태 로직 구성 가능
```

**Composite Pattern (Layers)**
```
LayerManager가 레이어 계층 관리
- 알파 블렌딩 합성
- Z-Order 관리
- 복잡도 캡슐화
```

### State Flow

```
User Input
    ↓
Tool.onDown/onMove/onUp
    ↓
CanvasCommand
    ↓
HistoryManager.execute
    ↓
DotKitState (new)
    ↓
Compose Recomposition
    ↓
DotKitCanvas Render
```

### Module Dependencies

```
sample (Android/Desktop/iOS/Wasm)
    ↓
dotkit-compose (UI)
    ↓
dotkit-core (Engine)
    ↓
kotlinx-coroutines-core
```

## Performance Characteristics

| 연산 | 시간복잡도 | 공간복잡도 |
|-----|-----------|-----------|
| 픽셀 그리기 | O(1) | O(1) |
| 브러시 스트로크 | O(n) | Undo용 O(n) |
| 레이어 합성 | O(w × h × layers) | O(w × h) |
| Undo/Redo | O(1) | O(history) |
| 줌/팬 | O(1) | O(1) |

**최적화**
- 보이는 레이어만 합성
- 커맨드는 델타만 저장
- 캐시 효율 좋은 `IntArray` 픽셀 버퍼
- 그리드는 `zoom >= 4f`에서만 렌더

## Algorithm Implementations

### Bresenham Line
LineTool/BrushTool에서 사용.

```kotlin
private fun interpolatePixels(from: Point, to: Point): List<Point>
```

### Midpoint Circle
ShapeTool 원 그리기에서 사용.

```kotlin
private fun drawCircleStroke(start: Point, end: Point): List<Point>
```

### Alpha Blending
표준 over 연산자.

```kotlin
fun composite(width: Int, height: Int): IntArray
```

## Platform Support

| 플랫폼 | Min | Target | 상태 |
|-------|-----|--------|------|
| Android | API 24 | API 35 | Stable |
| JVM Desktop | Java 17 | Java 17 | Stable |
| iOS | 12.0 | 17.0 | Stable |
| Web (Wasm) | Modern | Wasm-gc | Experimental |

## Technical Stack

- **Kotlin**: 2.1.0
- **Compose Multiplatform**: 1.9.3
- **Kotlin Coroutines**: 1.9.0
- **Android compileSdk**: 35 / **minSdk**: 24
- **JVM Target**: 17

## Project Structure

```
DotKit/
├── dotkit-core/
│   └── src/{commonMain,androidMain,desktopMain,iosMain,wasmJsMain}
├── dotkit-compose/
│   └── src/{commonMain,androidMain,desktopMain,iosMain,wasmJsMain}
├── sample/
│   └── src/{commonMain,androidMain,desktopMain,iosMain,wasmJsMain}
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Building and Running

### Sample Apps

**Android**
```bash
# 디버그 빌드
./gradlew :sample:assembleDebug
# 기기에 설치
./gradlew :sample:installDebug
```

**Desktop (JVM)**
```bash
# Desktop 앱 실행
./gradlew :sample:run
```

**iOS** (macOS required)
```bash
# iOS 바이너리 빌드
./gradlew :sample:iosX64Binaries
# Xcode에서 iosApp 프로젝트 열기
```

**Web (Wasm)**
```bash
# Development 모드 (빠른 빌드, 디버깅용)
./gradlew :sample:wasmJsBrowserDevelopmentRun

# Production 모드 (최적화 빌드)
./gradlew :sample:wasmJsBrowserProductionRun

# 빌드만 하기 (실행 없이)
./gradlew :sample:wasmJsBrowserDevelopmentWebpack
```

### Publishing Modules

```bash
./gradlew :dotkit-core:publishToMavenLocal
./gradlew :dotkit-compose:publishToMavenLocal
```

## Advanced Usage

### Custom Tool

```kotlin
class CustomTool : Tool {
    override val name = "Custom"
    override val supportsPreview = true

    override fun onDown(state: DotKitState, point: Point, color: Int): ToolState? =
        DefaultToolState(point, point, color)

    override fun onMove(state: DotKitState, point: Point, color: Int, toolState: ToolState?) =
        (toolState as? DefaultToolState)?.copy(currentPoint = point)

    override fun onUp(state: DotKitState, point: Point, color: Int, toolState: ToolState?) =
        null // 커스텀 CanvasCommand 반환

    override fun getPreviewPixels(toolState: ToolState?) = emptyList<Pair<Point, Int>>()
}
```

### Custom Command

```kotlin
class FloodFillCommand(
    private val layerId: String,
    private val startPoint: Point,
    private val fillColor: Int
) : CanvasCommand {
    private lateinit var previousPixels: Map<Point, Int>

    override fun execute(state: DotKitState): DotKitState {
        val layer = state.layerManager.findLayer(layerId) ?: return state
        val filled = floodFill(layer, startPoint, fillColor)
        previousPixels = filled
        return state.updateLayer(layerId) { l ->
            l.apply { filled.forEach { (p, c) -> setPixel(p.x, p.y, c) } }
        }
    }

    override fun undo(state: DotKitState): DotKitState =
        state.updateLayer(layerId) { l ->
            l.apply { previousPixels.forEach { (p, c) -> setPixel(p.x, p.y, c) } }
        }
}
```

### Layer Blending Modes (예시)

```kotlin
class CustomLayerManager : LayerManager() {
    override fun composite(width: Int, height: Int): IntArray {
        // multiply / screen / overlay 등 사용자 정의 블렌딩
        return super.composite(width, height)
    }
}
```

## Testing

```bash
./gradlew test
./gradlew :dotkit-core:desktopTest
./gradlew :dotkit-core:testDebugUnitTest
```

## Contributing

- Kotlin 코딩 컨벤션 준수
- 모든 테스트 통과
- 신규 기능에 테스트 포함
- 공개 API KDoc 작성
- 플랫폼별 코드는 expect/actual 패턴 사용

## License

MIT License - 자유롭게 사용 및 수정 가능합니다.

## Package

`io.github.kez.dotkit`

---

Kotlin Multiplatform & Compose Multiplatform 기반
