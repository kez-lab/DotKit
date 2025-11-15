package io.github.kez.dotkit.sample.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 색상 선택 다이얼로그
 */
@Composable
fun ColorPickerDialog(
    currentColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎨 색상 선택") },
        text = {
            val colors = listOf(
                0xFF000000.toInt() to "검정",
                0xFFFFFFFF.toInt() to "흰색",
                0xFFFF0000.toInt() to "빨강",
                0xFF00FF00.toInt() to "초록",
                0xFF0000FF.toInt() to "파랑",
                0xFFFFFF00.toInt() to "노랑",
                0xFFFF00FF.toInt() to "마젠타",
                0xFF00FFFF.toInt() to "시안",
                0xFF808080.toInt() to "회색",
                0xFFFFA500.toInt() to "주황",
                0xFF800080.toInt() to "보라",
                0xFFA52A2A.toInt() to "갈색"
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(colors) { (color, name) ->
                    val isSelected = color == currentColor
                    Button(
                        onClick = {
                            onColorSelected(color)
                            onDismiss()
                        },
                        modifier = Modifier
                            .aspectRatio(1f)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(color)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = name,
                            color = if (color == 0xFFFFFFFF.toInt() || color == 0xFFFFFF00.toInt()) {
                                Color.Black
                            } else {
                                Color.White
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}
