package io.github.kez.dotkit.export

/**
 * Web (Wasm) 플랫폼 이미지 내보내기 구현
 *
 * Note: Wasm에서는 브라우저 DOM API에 직접 접근이 제한되므로,
 * 실제 사용 시에는 JavaScript interop을 통해 구현해야 합니다.
 */
actual object ImageExporter {
    /**
     * PNG 이미지를 ByteArray로 내보내기
     *
     * Wasm에서는 제한적인 구현입니다.
     * 실제 사용을 위해서는 JavaScript interop 필요합니다.
     */
    actual fun exportPNG(pixels: IntArray, width: Int, height: Int): ByteArray {
        // 간단한 PNG 헤더와 픽셀 데이터를 포함한 최소 구현
        // 실제 프로덕션에서는 JS interop을 통해 Canvas API 사용 권장

        // TODO: JavaScript interop을 통한 실제 PNG 생성 구현
        throw UnsupportedOperationException(
            "PNG export in Wasm requires JavaScript interop. " +
            "Please use exportDataURL() or implement JS bridge."
        )
    }

    /**
     * PNG 이미지를 Data URL로 내보내기
     *
     * Wasm에서는 JavaScript interop을 통해 구현해야 합니다.
     */
    actual fun exportDataURL(pixels: IntArray, width: Int, height: Int): String {
        // TODO: JavaScript interop을 통한 Canvas API 사용 구현
        // 예시:
        // js("createCanvasDataURL")(pixels, width, height)

        throw UnsupportedOperationException(
            "Data URL export in Wasm requires JavaScript interop. " +
            "Please implement JS bridge with Canvas API."
        )
    }
}

/**
 * JavaScript interop helper (사용 예시)
 *
 * JavaScript 측에서 다음과 같은 함수를 제공해야 합니다:
 *
 * ```javascript
 * function createCanvasDataURL(pixels, width, height) {
 *   const canvas = document.createElement('canvas');
 *   canvas.width = width;
 *   canvas.height = height;
 *
 *   const ctx = canvas.getContext('2d');
 *   const imageData = ctx.createImageData(width, height);
 *
 *   // ARGB를 RGBA로 변환
 *   for (let i = 0; i < pixels.length; i++) {
 *     const argb = pixels[i];
 *     const offset = i * 4;
 *     imageData.data[offset + 0] = (argb >> 16) & 0xFF; // R
 *     imageData.data[offset + 1] = (argb >> 8) & 0xFF;  // G
 *     imageData.data[offset + 2] = argb & 0xFF;         // B
 *     imageData.data[offset + 3] = (argb >> 24) & 0xFF; // A
 *   }
 *
 *   ctx.putImageData(imageData, 0, 0);
 *   return canvas.toDataURL('image/png');
 * }
 * ```
 */
