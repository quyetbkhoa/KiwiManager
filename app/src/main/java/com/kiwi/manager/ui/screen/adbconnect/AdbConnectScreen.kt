package com.kiwi.manager.ui.screen.adbconnect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbConnectScreen(
    viewModel: AdbConnectViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ADB Connection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Instructions
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Instructions", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Connect watch and phone to the same Wi-Fi network.")
                    Text("2. Enable Developer Options and Wireless Debugging on your watch.")
                    Text("3. Enter the IP address and port displayed on your watch.")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Input Fields
            OutlinedTextField(
                value = uiState.ipAddress,
                onValueChange = { viewModel.updateIp(it) },
                label = { Text("IP Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = uiState.port,
                onValueChange = { viewModel.updatePort(it) },
                label = { Text("Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status and Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isConnected) Color(0xFF4CAF50) else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isConnected) "Connected" else "Disconnected")
                }
                
                Row {
                    if (uiState.isConnected) {
                        Button(onClick = { viewModel.disconnect() }) {
                            Text("Disconnect")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { /* Implement test connection if needed */ },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Test")
                        }
                        Button(
                            onClick = { viewModel.connect() },
                            enabled = !uiState.isConnecting
                        ) {
                            if (uiState.isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Connect")
                            }
                        }
                    }
                }
            }
            
            if (uiState.connectionError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.connectionError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // Saved Devices
            if (uiState.savedDevices.isNotEmpty()) {
                Text("Saved Devices", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                    items(uiState.savedDevices) { device ->
                        SuggestionChip(
                            onClick = { viewModel.selectSavedDevice(device) },
                            label = { Text("${device.host}:${device.port}") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Log Console
            Text("ADB Log", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            
            LaunchedEffect(uiState.log.size) {
                if (uiState.log.isNotEmpty()) {
                    coroutineScope.launch {
                        listState.animateScrollToItem(uiState.log.size - 1)
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
                        .background(Color(0xFF1E1E1E))
                        .padding(8.dp)
                ) {
                    items(uiState.log) { logLine ->
                        Text(
                            text = logLine,
                            color = Color(0xFF00FF00),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
