package com.example.breathingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.breathingapp.ui.navigation.AppNavigation
import com.example.breathingapp.theme.BreathingAppTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import com.example.breathingapp.data.SettingsRepository

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.statusBarColor = android.graphics.Color.TRANSPARENT
    window.navigationBarColor = 0xFF0D1411.toInt()

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        val layoutParams = window.attributes
        layoutParams.layoutInDisplayCutoutMode =
            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.attributes = layoutParams
    }
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
        window.isStatusBarContrastEnforced = false
    }
    setContent {
      val context = LocalContext.current
      val settingsRepository = SettingsRepository(context)
      val settings by settingsRepository.settingsFlow.collectAsState(initial = com.example.breathingapp.data.AppSettings())
      
      val isDarkTheme = if (settings.isDarkMode || settings.backgroundUri != null) true else isSystemInDarkTheme()

      BreathingAppTheme(darkTheme = isDarkTheme) {
        AppNavigation()
      }
    }
  }
}
