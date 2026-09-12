package com.example.ui.screens.ai

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.local.entity.UserEntity
import com.example.ui.screens.ai.workspace.AiAssistantWorkspace
import com.example.ui.viewmodel.AiAssistantViewModel

/**
 * Phase 6 AI Assistant Screen Entry Point.
 * Hosts the clean, modern conversational AI workspace.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: AiAssistantViewModel,
    currentUser: UserEntity?,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(currentUser) {
        viewModel.setCurrentUser(currentUser)
    }

    AiAssistantWorkspace(
        viewModel = viewModel,
        currentUser = currentUser,
        onOpenDrawer = onOpenDrawer,
        modifier = modifier
    )
}
