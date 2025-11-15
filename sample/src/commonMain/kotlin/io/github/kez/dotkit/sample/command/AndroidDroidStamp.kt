// file: io/github/kez/dotkit/sample/command/AndroidHeadStamp.kt
package io.github.kez.dotkit.sample.command

import io.github.kez.dotkit.history.DrawPixelsCommand
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.max

/**
 * Android "헤드" 아이콘 픽셀 스탬프
 * - 반원형 머리, 대각선 안테나 2개, 원형 눈 2개
 * - baseSize=64 권장(더 크게 그리고 싶으면 scale만 키워도 선명)
 */
object AndroidDroidStamp {
    private val ANDROID_GREEN = 0xFF34C759.toInt() // 현대 로고 톤에 가까운 초록 (원본 대비 약간 선명)
    private val WHITE = 0xFFFFFFFF.toInt()

    // ---- 최소 유틸 ----
    private fun rect(ox:Int, oy:Int, s:Int, x:Int, y:Int, w:Int, h:Int, c:Int)
            = ArrayList<DrawPixelsCommand.PixelPaint>(w*h*s*s).also { out ->
        repeat(h*s){dy-> repeat(w*s){dx->
            out += DrawPixelsCommand.PixelPaint(ox + x*s + dx, oy + y*s + dy, c)
        }}
    }

    /** 원(가득 채우기) */
    private fun fillCircle(
        ox:Int, oy:Int, s:Int,
        cx:Int, cy:Int, r:Int, c:Int
    ): List<DrawPixelsCommand.PixelPaint> {
        val out = mutableListOf<DrawPixelsCommand.PixelPaint>()
        val r2 = r * r
        for (yy in (cy - r)..(cy + r)) {
            val dy = yy - cy
            val dxMaxSq = r2 - dy*dy
            if (dxMaxSq < 0) continue
            val dxMax = sqrt(dxMaxSq.toDouble()).toInt()
            out += rect(ox, oy, s, cx - dxMax, yy, dxMax*2 + 1, 1, c)
        }
        return out
    }

    /** 상단 반원(아랫변이 평평) */
    private fun fillSemiCircleTop(
        ox:Int, oy:Int, s:Int,
        cx:Int, cyFlat:Int, r:Int, c:Int
    ): List<DrawPixelsCommand.PixelPaint> {
        // 중심 (cx, cyFlat) 를 기준으로 y는 위쪽만, 아래(=cyFlat)는 평평한 바닥
        val out = mutableListOf<DrawPixelsCommand.PixelPaint>()
        val r2 = r * r
        for (yy in (cyFlat - r)..cyFlat) {
            val dy = yy - cyFlat
            val dxMaxSq = r2 - dy*dy
            if (dxMaxSq < 0) continue
            val dxMax = sqrt(dxMaxSq.toDouble()).toInt()
            out += rect(ox, oy, s, cx - dxMax, yy, dxMax*2 + 1, 1, c)
        }
        return out
    }

    /** 1px(또는 n px) 대각선 */
    private fun line(
        ox:Int, oy:Int, s:Int,
        x0:Int, y0:Int, x1:Int, y1:Int, c:Int, thick:Int = 1
    ): List<DrawPixelsCommand.PixelPaint> {
        val out = mutableListOf<DrawPixelsCommand.PixelPaint>()
        var x = x0; var y = y0
        val dx = abs(x1 - x0); val sx = if (x0 < x1) 1 else -1
        val dy = -abs(y1 - y0); val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            out += rect(ox, oy, s, x, y, thick, 1, c)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
        return out
    }

    /**
     * 픽셀 목록 생성
     * originX/Y: 좌상단 기준
     * baseSize: 헤드 전체 폭 기준(권장 64)
     * scale: 픽셀 확장 배수
     */
    fun buildPixels(
        originX:Int = 0, originY:Int = 0,
        baseSize:Int = 64, scale:Int = 1
    ): List<DrawPixelsCommand.PixelPaint> {
        require(baseSize >= 32) { "baseSize는 32 이상 권장" }
        val s = scale
        val W = baseSize

        // 반지름/기하 비율(안드로이드 헤드 형태에 맞춤)
        val rHead = (W * 0.48f).toInt()             // 머리 반경(거의 전폭/2)
        val cx = W / 2                               // 중앙 x
        val cyFlat = rHead                           // 반원 바닥 y (origin Y 상대)

        val paints = mutableListOf<DrawPixelsCommand.PixelPaint>()

        // 1) 반원 머리
        paints += fillSemiCircleTop(originX, originY, s, cx, cyFlat, rHead, ANDROID_GREEN)

        // 2) 눈 (작은 원 2개)
        val eyeR = max(2, (W * 0.035f).toInt())     // 2~3 px 정도
        val eyeOffsetX = (rHead * 0.52f).toInt()
        val eyeY = (cyFlat - rHead * 0.35f).toInt()
        paints += fillCircle(originX, originY, s, cx - eyeOffsetX, eyeY, eyeR, WHITE)
        paints += fillCircle(originX, originY, s, cx + eyeOffsetX, eyeY, eyeR, WHITE)

        // 3) 안테나 (머리 가장자리에서 시작해 바깥 위로)
        val antLen = (W * 0.35f).toInt()
        val leftBaseX  = (cx - rHead * 0.70f).toInt()
        val rightBaseX = (cx + rHead * 0.70f).toInt()
        val baseY = (cyFlat - rHead + 2)
        // 좌/우 각각 살짝 바깥으로 기울이기
        paints += line(originX, originY, s,
            leftBaseX, baseY + 1,
            leftBaseX - (antLen * 0.35f).toInt(), baseY - antLen, ANDROID_GREEN, thick = 1
        )
        paints += line(originX, originY, s,
            rightBaseX, baseY + 1,
            rightBaseX + (antLen * 0.35f).toInt(), baseY - antLen, ANDROID_GREEN, thick = 1
        )

        return paints
    }

    /** 바로 실행 가능한 배치 커맨드 */
    fun buildCommand(
        layerId:String,
        originX:Int = 0, originY:Int = 0,
        baseSize:Int = 64, scale:Int = 1
    ) = DrawPixelsCommand(layerId, buildPixels(originX, originY, baseSize, scale))
}
