package io.github.kez.dotkit.sample.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kez.dotkit.compose.DotKitController

/**
 * 레이어 관리 다이얼로그
 */
@Composable
fun LayerManagerDialog(
    controller: DotKitController,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📚 레이어 관리") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 레이어 추가 버튼
                Button(
                    onClick = { controller.addLayer("레이어 ${controller.state.layers.size + 1}") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("➕ 레이어 추가")
                }

                // 레이어 목록
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(controller.state.layers.reversed()) { layer ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (layer.id == controller.state.activeLayerId) {
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            } else {
                                CardDefaults.cardColors()
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = layer.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row {
                                        // 가시성 토글
                                        IconButton(
                                            onClick = { controller.toggleLayerVisibility(layer.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text(
                                                if (layer.visible) "👁" else "🚫",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        // 레이어 선택
                                        if (layer.id != controller.state.activeLayerId) {
                                            IconButton(
                                                onClick = { controller.setActiveLayer(layer.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Text("✓", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }

                                // 불투명도 슬라이더
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "불투명도: ${(layer.opacity * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.width(100.dp)
                                    )
                                    Slider(
                                        value = layer.opacity,
                                        onValueChange = { controller.setLayerOpacity(layer.id, it) },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
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
