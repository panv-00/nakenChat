package com.example.nakenchat

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var editTextMessage: EditText
    private val messages = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageAdapter

    private lateinit var buttonW: Button
    private lateinit var buttonE: Button
    private lateinit var buttonT: Button
    private lateinit var buttonPrivate: Button
    private lateinit var buttonPercent: Button
    private lateinit var buttonQ: Button

    private var chatService: ChatService? = null
    private var isBound = false
    private lateinit var username: String

    private val POST_NOTIFICATION_REQUEST_CODE = 1001

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ChatService.ChatBinder
            chatService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            chatService = null
            isBound = false
        }
    }

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val rawMessage = intent?.getStringExtra("message")
            if (rawMessage != null) {
                val message = Message.parseRawMessage(rawMessage, username)
                messages.add(message)
                messageAdapter.notifyItemInserted(messages.size - 1)
                recyclerViewMessages.scrollToPosition(messages.size - 1)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        username = intent.getStringExtra("USERNAME") ?: ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), POST_NOTIFICATION_REQUEST_CODE)
            } else {
                startAndBindChatService()
            }
        } else {
            startAndBindChatService()
        }

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages)
        editTextMessage = findViewById(R.id.editTextMessage)

        messageAdapter = MessageAdapter(messages)
        recyclerViewMessages.adapter = messageAdapter
        recyclerViewMessages.layoutManager = LinearLayoutManager(this)

        editTextMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val message = editTextMessage.text.toString()
                if (message.isNotBlank()) {
                    sendMessage(message)
                    editTextMessage.text.clear()
                }
                true
            } else {
                false
            }
        }

        buttonW = findViewById(R.id.buttonW)
        buttonE = findViewById(R.id.buttonE)
        buttonT = findViewById(R.id.buttonT)
        buttonPrivate = findViewById(R.id.buttonPrivate)
        buttonPercent = findViewById(R.id.buttonPercent)
        buttonQ = findViewById(R.id.buttonQ)

        buttonW.setOnClickListener { sendMessage(".w") }
        buttonE.setOnClickListener { sendMessage(".e") }
        buttonT.setOnClickListener { sendMessage(".t") }
        buttonPrivate.setOnClickListener {
            editTextMessage.setText(".p ")
            editTextMessage.setSelection(editTextMessage.text.length)
        }
        buttonPercent.setOnClickListener {
            editTextMessage.setText("% ")
            editTextMessage.setSelection(editTextMessage.text.length)
        }
        buttonQ.setOnClickListener {
            sendMessage(".q")
            thread {
                Thread.sleep(2000)
                runOnUiThread {
                    finishAffinity()
                    exitProcess(0)
                }
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, IntentFilter("NewMessage"))
    }

    private fun startAndBindChatService() {
        val password = intent.getStringExtra("PASSWORD") ?: ""
        val serverIP = intent.getStringExtra("SERVER") ?: "10.0.2.2"
        val serverPORT = intent.getIntExtra("PORT", 6667)
        val useSSL = intent.getBooleanExtra("SSL", false)

        val serviceIntent = Intent(this, ChatService::class.java).apply {
            putExtra("USERNAME", username)
            putExtra("PASSWORD", password)
            putExtra("SERVER", serverIP)
            putExtra("PORT", serverPORT)
            putExtra("SSL", useSSL)
        }
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == POST_NOTIFICATION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startAndBindChatService()
            } else {
                Toast.makeText(this, "Notification permission is required for the chat to stay connected.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun sendMessage(message: String) {
        if (isBound) {
            chatService?.sendMessage(message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
    }
}