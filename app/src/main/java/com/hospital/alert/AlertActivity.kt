package com.hospital.alert

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.util.*

class AlertActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private var alertData: JSONObject? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on and full screen
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        
        // Get alert data
        val alertJson = intent.getStringExtra("alertData")
        alertData = try { JSONObject(alertJson) } catch (e: Exception) { null }
        
        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speakAnnouncementIfNeeded()
            }
        }
        
        setContent {
            AlertScreen()
        }
    }
    
    @Composable
    fun AlertScreen() {
        val backgroundColor = remember { 
            Color(android.graphics.Color.parseColor(alertData?.optString("codeColor") ?: "#3B82F6"))
        }
        val codeName = remember { alertData?.optString("codeName") ?: "EMERGENCY ALERT" }
        val locationName = remember { alertData?.optString("locationName") ?: "Unknown Location" }
        val message = remember { alertData?.optString("message") }
        val priority = remember { alertData?.optString("priority") ?: "HIGH" }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .clickable { 
                    tts.stop()
                    finish() 
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Alert Icon
                Text(
                    text = "🚨",
                    fontSize = 80.sp,
                    textAlign = TextAlign.Center
                )
                
                // Code Name
                Text(
                    text = codeName,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                // Location
                Text(
                    text = locationName,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                // Message
                if (!message.isNullOrEmpty()) {
                    Text(
                        text = message,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                // Priority
                Text(
                    text = "Priority: ${priority.uppercase()}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                // Instructions
                Text(
                    text = "Tap anywhere to acknowledge",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    private fun speakAnnouncementIfNeeded() {
        if (alertData?.optBoolean("voiceEnabled") == true) {
            val voiceText = alertData?.optString("voiceText")
            if (!voiceText.isNullOrEmpty()) {
                tts.speak(voiceText, TextToSpeech.QUEUE_FLUSH, null, "hospital_alert")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }
}
