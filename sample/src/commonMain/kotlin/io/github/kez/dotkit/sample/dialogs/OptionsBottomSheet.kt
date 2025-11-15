package io.github.kez.dotkit.sample.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 옵션 선택 바텀시트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsBottomSheet(
    onDismiss: () -> Unit,
    onToolsClick: () -> Unit,
    onColorsClick: () -> Unit,
    onBrushClick: () -> Unit,
    onLayersClick: () -> Unit,
    onGridToggle: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "도구 및 옵션",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OptionCard(
                        icon = "🎨",
                        label = "도구",
                        onClick = {
                            onDismiss()
                            onToolsClick()
                        }
                    )
                }
                item {
                    OptionCard(
                        icon = "🎨",
                        label = "색상",
                        onClick = {
                            onDismiss()
                            onColorsClick()
                        }
                    )
                }
                item {
                    OptionCard(
                        icon = "🖌️",
                        label = "브러시",
                        onClick = {
                            onDismiss()
                            onBrushClick()
                        }
                    )
                }
                item {
                    OptionCard(
                        icon = "📚",
                        label = "레이어",
                        onClick = {
                            onDismiss()
                            onLayersClick()
                        }
                    )
                }
                item {
                    OptionCard(
                        icon = "📐",
                        label = "격자",
                        onClick = {
                            onGridToggle()
                            onDismiss()
                        }
                    )
                }
                item {
                    OptionCard(
                        icon = "🗑️",
                        label = "지우기",
                        onClick = {
                            onClear()
                            onDismiss()
                        }
                    )
                }
                item {
                    OptionCard(
                        icon = "💾",
                        label = "저장",
                        onClick = {
                            onDismiss()
                            onSave()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OptionCard(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
