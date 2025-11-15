package io.github.kez.dotkit.sample

import androidx.compose.runtime.Composable

/**
 * DotKit 샘플 애플리케이션
 *
 * 모바일 최적화 레이아웃을 사용합니다:
 * - 전체 화면 캔버스가 메인 UI
 * - 도구/색상/레이어는 다이얼로그로 접근
 * - FloatingActionButton으로 모든 기능 제공
 */
@Composable
fun App() {
    // 모바일 최적화 레이아웃 사용
    MobileApp()
}
