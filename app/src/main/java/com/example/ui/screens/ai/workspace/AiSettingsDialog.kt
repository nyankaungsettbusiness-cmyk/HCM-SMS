package com.example.ui.screens.ai.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AiAssistantViewModel

/**
 * Compact AI Settings Modal Dialog for Phase 6.
 * Accessible exclusively via TopAppBar `⋮ → AI Settings`.
 */
@Composable
fun AiSettingsDialog(
    viewModel: AiAssistantViewModel?,
    onDismiss: () -> Unit
) {
    var selectedModel by remember { mutableStateOf("gemini-2.5-flash") }
    var strictGrounding by remember { mutableStateOf(true) }
    var temperature by remember { mutableFloatStateOf(0.3f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("ai_settings_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("AI Assistant Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Model Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("AI Model Architecture", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    listOf(
                        "gemini-2.5-flash" to "Gemini 2.5 Flash (Fast & Grounded)",
                        "gemini-2.5-pro" to "Gemini 2.5 Pro (Deep Reasoning)"
                    ).forEach { (id, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedModel == id,
                                onClick = { selectedModel = id }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, fontSize = 12.sp)
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Grounding Strictness
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict Curriculum Grounding", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Ensure 100% textbook traceability and zero hallucination.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = strictGrounding,
                        onCheckedChange = { strictGrounding = it }
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Creativity / Temperature
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Generation Temperature", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text(String.format("%.1f", temperature), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    }
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0.0f..1.0f,
                        steps = 9
                    )
                    Text("Lower values ensure strict adherence to official syllabus questions.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
