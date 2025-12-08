package com.example.nakenchat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*
import kotlin.concurrent.thread

class ChatService : Service() {

    private val CHANNEL_ID = "ChatServiceChannel"
    private val NOTIFICATION_ID = 1
    private lateinit var socket: Socket
    private lateinit var writer: PrintWriter
    private lateinit var reader: BufferedReader
    private var isRunning = true

    inner class ChatBinder : Binder() {
        fun getService(): ChatService = this@ChatService
    }

    private val binder = ChatBinder()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = createNotification("Connecting...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val username = intent.getStringExtra("USERNAME") ?: ""
        val password = intent.getStringExtra("PASSWORD") ?: ""
        val serverIP = intent.getStringExtra("SERVER") ?: "10.0.2.2"
        val serverPORT = intent.getIntExtra("PORT", 6667)
        val useSSL = intent.getBooleanExtra("SSL", false)

        connectToServer(username, password, serverIP, serverPORT, useSSL)

        return START_STICKY
    }

    private fun connectToServer(username: String, password: String, serverIP: String, serverPORT: Int, useSSL: Boolean) {
        thread {
            try {
                socket = if (useSSL) {
                    val factory = createTrustAllSSLSocketFactory()
                    factory.createSocket(serverIP, serverPORT) as SSLSocket
                } else {
                    Socket(serverIP, serverPORT)
                }

                writer = PrintWriter(socket.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                val initialMessage = if (password.isNotEmpty()) ".n $username=$password" else ".n $username"
                writer.println(initialMessage)

                val helloMessage = if (useSSL) "% used an Android device to come here (SSL)!!" else "% used an Android device to come here!!"
                writer.println(helloMessage)

                updateNotification("Connected to server")

                while (isRunning) {
                    val line = reader.readLine() ?: break
                    val intent = Intent("NewMessage")
                    intent.putExtra("message", line)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    fun sendMessage(message: String) {
        thread {
            if (::writer.isInitialized) {
                writer.println(message)
                writer.flush()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        thread {
            try {
                if (::writer.isInitialized) {
                    writer.println(".q")
                    writer.flush()
                }
                if (::socket.isInitialized && !socket.isClosed) {
                    socket.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NakenChat")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Chat Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    private fun createTrustAllSSLSocketFactory(): SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            }
        )
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext.socketFactory
    }
}