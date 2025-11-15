package io.github.kez.dotkit.tools

import io.github.kez.dotkit.canvas.CanvasState
import io.github.kez.dotkit.common.Point
import io.github.kez.dotkit.history.CanvasCommand

/**
 * 모든 드로잉 도구의 기본 인터페이스
 */
interface Tool {
    /**
     * 도구 이름
     */
    val name: String

    /**
     * 도구 시작 (마우스 다운 또는 터치 시작)
     *
     * @param state 현재 캔버스 상태
     * @param point 시작 지점
     * @param color 사용할 색상
     * @return 생성된 도구 상태 (상태가 있는 도구의 경우)
     */
    fun onDown(state: CanvasState, point: Point, color: Int): ToolState?

    /**
     * 도구 이동 (드래그 중)
     *
     * @param state 현재 캔버스 상태
     * @param point 현재 지점
     * @param color 사용할 색상
     * @param toolState 이전 도구 상태
     * @return 업데이트된 도구 상태
     */
    fun onMove(state: CanvasState, point: Point, color: Int, toolState: ToolState?): ToolState?

    /**
     * 도구 종료 (마우스 업 또는 터치 종료)
     *
     * @param state 현재 캔버스 상태
     * @param point 종료 지점
     * @param color 사용할 색상
     * @param toolState 최종 도구 상태
     * @return 실행할 커맨드 (없으면 null)
     */
    fun onUp(state: CanvasState, point: Point, color: Int, toolState: ToolState?): CanvasCommand?

    /**
     * 도구가 프리뷰를 지원하는지 여부
     */
    val supportsPreview: Boolean get() = false

    /**
     * 프리뷰 렌더링을 위한 임시 픽셀 계산
     *
     * @param toolState 현재 도구 상태
     * @return 프리뷰할 픽셀 목록 (좌표, 색상)
     */
    fun getPreviewPixels(toolState: ToolState?): List<Pair<Point, Int>> = emptyList()
}

/**
 * 도구의 상태를 저장하는 인터페이스
 * 드래그 중에 도구의 임시 상태를 유지합니다.
 */
interface ToolState {
    /**
     * 시작 지점
     */
    val startPoint: Point

    /**
     * 현재/종료 지점
     */
    val currentPoint: Point

    /**
     * 사용 중인 색상
     */
    val color: Int

    /**
     * 영향받은 픽셀 (undo를 위한 백업)
     */
    val affectedPixels: List<Pair<Point, Int>>
}

/**
 * 기본 도구 상태 구현
 */
data class DefaultToolState(
    override val startPoint: Point,
    override val currentPoint: Point,
    override val color: Int,
    override val affectedPixels: List<Pair<Point, Int>> = emptyList()
) : ToolState

/**
 * 도구 타입 열거형
 */
enum class ToolType {
    BRUSH,      // 브러시
    LINE,       // 라인
    RECTANGLE,  // 사각형
    CIRCLE,     // 원
    ERASER,     // 지우개
    EYEDROPPER  // 스포이드
}
