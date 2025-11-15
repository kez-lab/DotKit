package io.github.kez.dotkit.common

/**
 * 2D 좌표를 나타내는 데이터 클래스
 */
data class Point(
    val x: Int,
    val y: Int
) {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)

    companion object {
        val ZERO = Point(0, 0)
    }
}

/**
 * 2D 오프셋을 나타내는 데이터 클래스 (Float)
 */
data class Offset(
    val x: Float,
    val y: Float
) {
    operator fun plus(other: Offset) = Offset(x + other.x, y + other.y)
    operator fun minus(other: Offset) = Offset(x - other.x, y - other.y)
    operator fun times(scale: Float) = Offset(x * scale, y * scale)

    companion object {
        val ZERO = Offset(0f, 0f)
    }
}
