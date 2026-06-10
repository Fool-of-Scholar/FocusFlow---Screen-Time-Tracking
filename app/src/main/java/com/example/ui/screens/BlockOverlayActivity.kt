package com.example.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity

class BlockOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val blockedAppName = intent.getStringExtra("BLOCKED_APP_NAME") ?: "App"

        setContent {
            MaterialTheme {
                BlockScreenContent(blockedAppName = blockedAppName) {
                    // Navigate back to FocusFlow home
                    val intent = Intent(this@BlockOverlayActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}

@Composable
fun BlockScreenContent(blockedAppName: String, onGoHome: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = com.example.R.drawable.cat_mascot_front_view),
                contentDescription = "Cat Mascot Front",
                modifier = Modifier.size(150.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "ACCESS DENIED 🛑",
                color = Color.Red,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "$blockedAppName is currently blocked.",
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Coach Master Kitty says: Time to focus on your goals!",
                color = Color.LightGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.7f).height(50.dp)
            ) {
                Text(text = "Return to FocusFlow", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
