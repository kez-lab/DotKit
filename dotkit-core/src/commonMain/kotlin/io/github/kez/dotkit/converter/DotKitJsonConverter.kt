package io.github.kez.dotkit.converter

import io.github.kez.dotkit.DotKitState
import io.github.kez.dotkit.layers.Layer
import io.github.kez.dotkit.layers.LayerManager

/**
 * DotKit JSON Converter
 *
 * Parses JSON strings into DotKitState.
 * Implements a simple recursive descent parser to avoid external dependencies.
 */
object DotKitJsonConverter {

    /**
     * Parses a JSON string into DotKitState.
     * Supports two formats:
     * 1. Full State: { "width": 16, "height": 16, "layers": [...] }
     * 2. Simple Grid: { "width": 8, "height": 8, "palette": [...], "data": [[...]] }
     */
    fun parse(json: String): DotKitState {
        val root = JsonParser(json).parse() as? Map<String, Any>
            ?: throw IllegalArgumentException("Invalid JSON root")

        val width = (root["width"] as? Number)?.toInt() ?: 32
        val height = (root["height"] as? Number)?.toInt() ?: 32

        // Check for "layers" (Full Format)
        if (root.containsKey("layers")) {
            val layersList = root["layers"] as? List<Map<String, Any>>
                ?: throw IllegalArgumentException("Invalid layers format")
            
            var layerManager = LayerManager()
            layersList.forEach { layerMap ->
                val name = layerMap["name"] as? String ?: "Layer"
                val pixels = layerMap["pixels"] as? List<Any> ?: emptyList()
                
                val layer = Layer.create(width, height, name)
                val pixelArray = IntArray(width * height)
                
                pixels.forEachIndexed { index, pixelValue ->
                    if (index < pixelArray.size) {
                        pixelArray[index] = parseColor(pixelValue)
                    }
                }
                
                layer.setPixels(pixelArray)
                
                // Optional properties
                if (layerMap.containsKey("visible")) {
                    // layer.visible = layerMap["visible"] as Boolean // Layer is immutable, need copy
                    // But Layer.create returns mutable? No, Layer is data class but has internal buffer.
                    // Wait, Layer is immutable wrapper around buffer?
                    // Let's check Layer.kt. It's a data class.
                    // We need to handle properties if we want to support them fully.
                    // For now, let's stick to basic pixel data.
                }
                
                layerManager = layerManager.addLayer(layer)
            }
            
            return DotKitState(
                width = width,
                height = height,
                layerManager = layerManager,
                activeLayerId = layerManager.getLayers().lastOrNull()?.id
            )
        }
        
        // Check for "data" and "palette" (Grid Format)
        if (root.containsKey("data") && root.containsKey("palette")) {
            val palette = (root["palette"] as? List<String>)?.map { parseColor(it) }
                ?: throw IllegalArgumentException("Invalid palette")
            
            val data = root["data"] as? List<List<Number>>
                ?: throw IllegalArgumentException("Invalid data format")
            
            val layer = Layer.create(width, height, "Imported")
            val pixelArray = IntArray(width * height)
            
            data.forEachIndexed { y, row ->
                row.forEachIndexed { x, colorIndex ->
                    if (x < width && y < height) {
                        val idx = colorIndex.toInt()
                        if (idx in palette.indices) {
                            pixelArray[y * width + x] = palette[idx]
                        }
                    }
                }
            }
            
            layer.setPixels(pixelArray)
            
            return DotKitState(
                width = width,
                height = height,
                layerManager = LayerManager().addLayer(layer),
                activeLayerId = layer.id
            )
        }

        throw IllegalArgumentException("Unknown JSON format")
    }

    private fun parseColor(value: Any): Int {
        return when (value) {
            is Number -> value.toInt()
            is String -> {
                if (value.startsWith("#")) {
                    // #RRGGBB or #AARRGGBB
                    val hex = value.substring(1)
                    if (hex.length == 6) {
                        ("FF$hex").toLong(16).toInt()
                    } else {
                        hex.toLong(16).toInt()
                    }
                } else {
                    // Try parsing as hex string without #
                    try {
                        if (value.length == 6) ("FF$value").toLong(16).toInt()
                        else value.toLong(16).toInt()
                    } catch (e: Exception) {
                        0 // Fail safe
                    }
                }
            }
            else -> 0
        }
    }

    // --- Simple JSON Parser Implementation ---

    private class JsonParser(private val json: String) {
        private var pos = 0
        private val length = json.length

        fun parse(): Any? {
            skipWhitespace()
            if (pos >= length) return null

            return when (json[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                else -> parseNumber()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            consume('{')
            val map = mutableMapOf<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                consume('}')
                return map
            }

            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                consume(':')
                val value = parse()
                map[key] = value

                skipWhitespace()
                if (peek() == '}') {
                    consume('}')
                    break
                }
                consume(',')
            }
            return map
        }

        private fun parseArray(): List<Any?> {
            consume('[')
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                consume(']')
                return list
            }

            while (true) {
                list.add(parse())
                skipWhitespace()
                if (peek() == ']') {
                    consume(']')
                    break
                }
                consume(',')
            }
            return list
        }

        private fun parseString(): String {
            consume('"')
            val sb = StringBuilder()
            while (pos < length) {
                val c = json[pos++]
                if (c == '"') break
                if (c == '\\') {
                    if (pos < length) {
                        when (val escaped = json[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 <= length) {
                                    val hex = json.substring(pos, pos + 4)
                                    sb.append(hex.toInt(16).toChar())
                                    pos += 4
                                }
                            }
                            else -> sb.append(escaped)
                        }
                    }
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }

        private fun parseNumber(): Number {
            val start = pos
            if (peek() == '-') pos++
            while (pos < length && json[pos].isDigit()) pos++
            if (pos < length && json[pos] == '.') {
                pos++
                while (pos < length && json[pos].isDigit()) pos++
            }
            val str = json.substring(start, pos)
            return if (str.contains('.')) str.toDouble() else str.toLong()
        }

        private fun parseBoolean(): Boolean {
            return if (json.startsWith("true", pos)) {
                pos += 4
                true
            } else {
                pos += 5
                false
            }
        }

        private fun parseNull(): Any? {
            pos += 4
            return null
        }

        private fun skipWhitespace() {
            while (pos < length && json[pos].isWhitespace()) pos++
        }

        private fun peek(): Char = if (pos < length) json[pos] else 0.toChar()

        private fun consume(expected: Char) {
            skipWhitespace()
            if (pos < length && json[pos] == expected) {
                pos++
            } else {
                throw IllegalArgumentException("Expected '$expected' at $pos but found '${if (pos < length) json[pos] else "EOF"}'")
            }
        }
    }
}
