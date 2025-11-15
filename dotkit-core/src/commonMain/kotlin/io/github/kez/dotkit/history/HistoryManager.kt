package io.github.kez.dotkit.history

import io.github.kez.dotkit.DotKitState

/**
 * 실행 취소/다시 실행을 관리하는 히스토리 매니저
 *
 * @param maxHistorySize 저장할 최대 히스토리 개수 (기본 50)
 */
class HistoryManager(
    private val maxHistorySize: Int = 50
) {
    private val undoStack = mutableListOf<CanvasCommand>()
    private val redoStack = mutableListOf<CanvasCommand>()

    /**
     * 실행 취소 가능 여부
     */
    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    /**
     * 다시 실행 가능 여부
     */
    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    /**
     * 실행 취소 스택 크기
     */
    val undoSize: Int
        get() = undoStack.size

    /**
     * 다시 실행 스택 크기
     */
    val redoSize: Int
        get() = redoStack.size

    /**
     * 새 명령 실행 및 히스토리에 추가
     *
     * @param state 현재 캔버스 상태
     * @param command 실행할 명령
     * @return 명령 실행 후의 새로운 캔버스 상태
     */
    fun execute(state: DotKitState, command: CanvasCommand): DotKitState {
        val newState = command.execute(state)

        // 실행 취소 스택에 추가
        undoStack.add(command)

        // 최대 크기 제한
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }

        // 새 명령 실행 시 다시 실행 스택 초기화
        redoStack.clear()

        return newState
    }

    /**
     * 실행 취소
     *
     * @param state 현재 캔버스 상태
     * @return 실행 취소 후의 캔버스 상태
     */
    fun undo(state: DotKitState): DotKitState {
        if (!canUndo) return state

        val command = undoStack.removeAt(undoStack.lastIndex)
        val newState = command.undo(state)

        // 다시 실행 스택에 추가
        redoStack.add(command)

        return newState
    }

    /**
     * 다시 실행
     *
     * @param state 현재 캔버스 상태
     * @return 다시 실행 후의 캔버스 상태
     */
    fun redo(state: DotKitState): DotKitState {
        if (!canRedo) return state

        val command = redoStack.removeAt(redoStack.lastIndex)
        val newState = command.execute(state)

        // 실행 취소 스택에 추가
        undoStack.add(command)

        return newState
    }

    /**
     * 히스토리 초기화
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    /**
     * 현재 히스토리 상태 정보
     */
    fun getHistoryInfo(): HistoryInfo {
        return HistoryInfo(
            undoCount = undoStack.size,
            redoCount = redoStack.size,
            maxSize = maxHistorySize,
            canUndo = canUndo,
            canRedo = canRedo
        )
    }
}

/**
 * 히스토리 상태 정보
 */
data class HistoryInfo(
    val undoCount: Int,
    val redoCount: Int,
    val maxSize: Int,
    val canUndo: Boolean,
    val canRedo: Boolean
)
