package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.MainScreen
import com.example.ui.theme.FocusFlowTheme
import com.example.ui.viewmodel.FocusViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import android.content.res.Configuration
import java.util.Locale

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: FocusViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Support full screen edge-to-edge rendering
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[FocusViewModel::class.java]

        setContent {
            val languagePref by viewModel.languageOptionPreference.collectAsState()
            val context = LocalContext.current
            
            val locale = when (languagePref) {
                "Español 🇪🇸", "Español" -> Locale("es")
                "Français 🇫🇷", "Français" -> Locale("fr")
                "Deutsch 🇩🇪", "Deutsch" -> Locale("de")
                else -> Locale("en")
            }
            
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            val localizedContext = context.createConfigurationContext(configuration)
            
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration
            ) {
                FocusFlowTheme {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
