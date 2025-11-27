package com.hospital.alert

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start the background service
        startAlertService()
        
        setContent {
            HospitalAlertTheme {
                SettingsScreen()
            }
        }
    }
    
    private fun startAlertService() {
        val intent = Intent(this, AlertListenerService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var locationName by remember { mutableStateOf(getLocationName(context)) }
    var isConnected by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Hospital Alert Agent",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        // Connection Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null
                )
                Column {
                    Text(
                        text = if (isConnected) "✅ Ready to Receive Alerts" else "❌ Not Connected",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isConnected) "Listening on port 3002" else "Check network connection",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Location Settings
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Location Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Location Name") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Emergency Room, ICU, Main Ward") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        saveLocationName(context, locationName)
                        Toast.makeText(context, "Configuration Saved", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Configuration")
                }
            }
        }
        
        // Instructions
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "How It Works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                listOf(
                    "• This agent listens on port 3002 for alerts",
                    "• Admin app sends alerts to port 3002 on your network",
                    "• When emergency alert is received, it displays fullscreen",
                    "• Voice announcements are controlled by admin app",
                    "• Tap anywhere on alert to acknowledge and close",
                    "• The agent runs in background after closing app"
                ).forEach { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun getLocationName(context: Context): String {
    val prefs = context.getSharedPreferences("hospital_prefs", Context.MODE_PRIVATE)
    return prefs.getString("location_name", "Mobile Device") ?: "Mobile Device"
}

private fun saveLocationName(context: Context, name: String) {
    val prefs = context.getSharedPreferences("hospital_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("location_name", name).apply()
}
