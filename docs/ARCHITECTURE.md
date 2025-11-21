# DotKit Architecture

This document details the internal architecture, design patterns, and performance characteristics of DotKit.

## Architecture Overview

### Design Patterns

**Immutable State Pattern**
```
All state transitions return a new instance via copy()
- Predictable state transitions
- Easy Undo/Redo implementation
- Inherently thread-safe
```

**Command Pattern**
```
All operations are CanvasCommand
- execute(): Apply change
- undo(): Revert change
- Easy composition of complex operations
```

**Strategy Pattern (Tools)**
```
Runtime tool switching via Tool interface
- Independent implementations
- Easy extension
- Stateless logic composition
```

**Composite Pattern (Layers)**
```
LayerManager manages the layer hierarchy
- Alpha blending composition
- Z-Order management
- Encapsulated complexity
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

| Operation | Time Complexity | Space Complexity |
|---|---|---|
| Pixel Draw | O(1) | O(1) |
| Brush Stroke | O(n) | O(n) for Undo |
| Layer Composite | O(w × h × layers) | O(w × h) |
| Undo/Redo | O(1) | O(history) |
| Zoom/Pan | O(1) | O(1) |

**Optimizations**
- **Zero-Allocation Drawing**: Minimized object creation using `IntArray` based batch pixel updates.
- **Optimized Composition**: Composites only visible layers and skips transparent pixels efficiently.
- **Memory Efficient**: Uses `IntArray` pixel buffers and delta-based command storage.
- **Smart Grid**: Renders grid only when `zoom >= 4f` to prevent overhead.

## Algorithm Implementations

### Bresenham Line
Used in `LineTool` and `BrushTool` for pixel-perfect lines.

```kotlin
private fun interpolatePixels(from: Point, to: Point): List<Point>
```

### Midpoint Circle
Used in `ShapeTool` for circle drawing.

```kotlin
private fun drawCircleStroke(start: Point, end: Point): List<Point>
```

### Alpha Blending
Standard over operator implementation.

```kotlin
fun composite(width: Int, height: Int): IntArray
```
