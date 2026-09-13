package com.kiwi.manager.ui.screen.appdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiwi.manager.domain.model.AppDisplayInfo
import com.kiwi.manager.domain.model.DownloadProgress
import com.kiwi.manager.domain.model.InstallStatus
import com.kiwi.manager.ui.component.StatusBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    viewModel: AppDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAdbConnect: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.app?.app?.name ?: "App Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "Error loading app",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.app != null) {
                AppDetailContent(
                    appDisplayInfo = uiState.app!!,
                    uiState = uiState,
                    onInstallPhone = { viewModel.installPhone() },
                    onInstallWatch = { viewModel.installWatch() },
                    onNavigateToAdbConnect = onNavigateToAdbConnect
                )
            }
        }
    }
}

@Composable
fun AppDetailContent(
    appDisplayInfo: AppDisplayInfo,
    uiState: AppDetailUiState,
    onInstallPhone: () -> Unit,
    onInstallWatch: () -> Unit,
    onNavigateToAdbConnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appDisplayInfo.app.name.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = appDisplayInfo.app.name, style = MaterialTheme.typography.headlineMedium)
                Text(text = appDisplayInfo.app.description, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Phone Section
        if (appDisplayInfo.app.phone != null) {
            PlatformCard(
                title = "📱 Phone",
                installedVersion = appDisplayInfo.phoneInstalled?.versionName ?: "Not installed",
                latestVersion = appDisplayInfo.app.phone.versionName,
                status = appDisplayInfo.phoneStatus,
                isInstalling = uiState.phoneInstalling,
                downloadProgress = uiState.phoneDownloadProgress,
                onInstallClick = onInstallPhone
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Watch Section
        if (appDisplayInfo.app.watch != null) {
            PlatformCard(
                title = "⌚ Watch",
                installedVersion = appDisplayInfo.watchInstalled?.versionName ?: "Not installed",
                latestVersion = appDisplayInfo.app.watch.versionName,
                status = appDisplayInfo.watchStatus,
                isInstalling = uiState.watchInstalling,
                downloadProgress = uiState.watchDownloadProgress,
                onInstallClick = onInstallWatch
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Requires ADB connection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onNavigateToAdbConnect) {
                    Text("Connect")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Install Log
        if (uiState.installLog.isNotEmpty()) {
            Text("Install Log", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            
            LaunchedEffect(uiState.installLog.size) {
                if (uiState.installLog.isNotEmpty()) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(uiState.installLog.size - 1)
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                ) {
                    items(uiState.installLog) { logLine ->
                        Text(
                            text = logLine,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformCard(
    title: String,
    installedVersion: String,
    latestVersion: String,
    status: InstallStatus,
    isInstalling: Boolean,
    downloadProgress: DownloadProgress?,
    onInstallClick: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                StatusBadge(status = status, latestVersion = latestVersion)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Installed: $installedVersion", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Latest: $latestVersion", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isInstalling) {
                if (downloadProgress != null) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.percent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${downloadProgress.percent}% · ${downloadProgress.downloadedMb} / ${downloadProgress.totalMb}",
                        style = MaterialTheme.typography.labelSmall
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Installing...", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                val buttonText = when (status) {
                    InstallStatus.NOT_INSTALLED -> "Install"
                    InstallStatus.UPDATE_AVAILABLE -> "Update"
                    InstallStatus.UP_TO_DATE -> "Reinstall"
                    InstallStatus.UNKNOWN -> "Install"
                }
                FilledTonalButton(
                    onClick = onInstallClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}
