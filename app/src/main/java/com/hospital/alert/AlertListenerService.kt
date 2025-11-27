package com.hospital.alert

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AlertListenerService : Service() {
    private val TAG = "AlertListenerService"
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(3, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    private var serviceJob: Job? = null
    private var isConnected = false

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        startWebSocketConnection()
    }

    private fun startForegroundService() {
        val channelId = "hospital_alert_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hospital Alert Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Listens for emergency alerts"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Hospital Alert Agent")
            .setContentText("Listening for emergency alerts...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .build()

        startForeground(1, notification)
    }

    private fun startWebSocketConnection() {
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    connectWebSocket()
                    delay(5000) // Check connection every 5 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "WebSocket connection error: ${e.message}")
                    delay(3000) // Wait before retry
                }
            }
        }
    }

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url("ws://192.168.1.100:3002") // Will be configurable
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                isConnected = true
                this@AlertListenerService.webSocket = webSocket
                
                // Send registration message
                val registration = JSONObject().apply {
                    put("type", "agent-registration")
                    put("device", "android")
                    put("location", getLocationName())
                }
                webSocket.send(registration.toString())
                
                updateNotification("Connected - Ready for alerts")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                handleIncomingMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Received bytes: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
                isConnected = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                isConnected = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                isConnected = false
                updateNotification("Disconnected - Retrying...")
            }
        }

        client.newWebSocket(request, listener)
    }

    private fun handleIncomingMessage(message: String) {
        try {
            val json = JSONObject(message)
            when (json.getString("type")) {
                "alert" -> {
                    val alertData = json.getJSONObject("data")
                    showEmergencyAlert(alertData)
                }
                "speak" -> {
                    val voiceData = json.getJSONObject("data")
                    speakAnnouncement(voiceData)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message: ${e.message}")
        }
    }

    private fun showEmergencyAlert(alertData: JSONObject) {
        val intent = Intent(this, AlertActivity::class.java).apply {
            putExtra("alertData", alertData.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        
        // Vibrate and sound
        vibrateDevice()
    }

    private fun vibrateDevice() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), 0))
            } else {
                vibrator.vibrate(longArrayOf(0, 500, 200, 500), -1)
            }
        }
    }

    private fun speakAnnouncement(voiceData: JSONObject) {
        // TTS implementation will be added
        Log.d(TAG, "Voice announcement requested: ${voiceData.getString("text")}")
    }

    private fun updateNotification(text: String) {
        val notification = Notification.Builder(this, "hospital_alert_channel")
            .setContentTitle("Hospital Alert Agent")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    private fun getLocationName(): String {
        val prefs = getSharedPreferences("hospital_prefs", Context.MODE_PRIVATE)
        return prefs.getString("location_name", "Mobile Device") ?: "Mobile Device"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        webSocket?.close(1000, "Service destroyed")
        client.dispatcher.executorService.shutdown()
    }
}
