package com.kiwi.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kiwi.manager.ui.navigation.KiwiNavGraph
import com.kiwi.manager.ui.theme.KiwiManagerTheme
import com.kiwi.manager.domain.model.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.DARK) }
            val navController = rememberNavController()
            
            KiwiManagerTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KiwiNavGraph(
                        navController = navController,
                        themeMode = themeMode,
                        onThemeChange = { themeMode = it }
                    )
                }
            }
        }
    }
}
