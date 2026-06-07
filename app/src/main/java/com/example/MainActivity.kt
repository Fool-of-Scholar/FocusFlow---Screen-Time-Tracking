package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.MainScreen
import com.example.ui.theme.FocusFlowTheme
import com.example.ui.viewmodel.FocusViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: FocusViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Support full screen edge-to-edge rendering
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[FocusViewModel::class.java]

        setContent {
            FocusFlowTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
