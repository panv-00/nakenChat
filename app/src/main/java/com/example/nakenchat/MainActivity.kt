package com.example.nakenchat

import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var socket: Socket
    private lateinit var writer: PrintWriter
    private lateinit var reader: BufferedReader

    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var editTextMessage: EditText

    private val messages = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageAdapter

    private lateinit var username: String
    private lateinit var password: String
    private lateinit var serverIP: String
    private var serverPORT: Int = 6666

    private lateinit var buttonW: Button
    private lateinit var buttonE: Button
    private lateinit var buttonT: Button
    private lateinit var buttonPrivate: Button
    private lateinit var buttonPercent: Button
    private lateinit var buttonQ: Button

    @Volatile
    private var isRunning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        username = intent.getStringExtra("USERNAME") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: ""
        serverIP = intent.getStringExtra("SERVER") ?: "10.0.2.2"
        serverPORT = intent.getIntExtra("PORT", 6666)

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

        buttonW.setOnClickListener {
            sendMessage(".w")
        }
        buttonE.setOnClickListener {
            sendMessage(".e")
        }
        buttonT.setOnClickListener {
            sendMessage(".t")
        }
        buttonPrivate.setOnClickListener {
            runOnUiThread {
                editTextMessage.setText(".p")
                editTextMessage.setSelection(editTextMessage.text.length)
            }
        }
        buttonPercent.setOnClickListener {
            runOnUiThread {
                editTextMessage.setText("%")
                editTextMessage.setSelection(editTextMessage.text.length)
            }
        }
        buttonQ.setOnClickListener {
            sendMessage(".q")
            // Delay 2 seconds then finish the app
            thread {
                Thread.sleep(2000)
                runOnUiThread {
                    finishAffinity()
                    exitProcess(0)
                }
            }
        }

        connectToServer()
    }

    private fun connectToServer() {
        thread {
            try {
                socket = Socket(serverIP, serverPORT)
                writer = PrintWriter(socket.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                val initialMessage = if (password.isNotEmpty()) {
                    ".n $username=$password"
                } else {
                    ".n $username"
                }

                writer.println(initialMessage)

                val helloMessage = "% used an Android device to come here!!"
                writer.println(helloMessage)

                runOnUiThread {
                    Toast.makeText(this, "Connected to server", Toast.LENGTH_SHORT).show()
                }

                while (isRunning) {
                    val line = reader.readLine() ?: break
                    val message = Message.parseRawMessage(line, username)
                    runOnUiThread {
                        messages.add(message)
                        messageAdapter.notifyItemInserted(messages.size - 1)
                        recyclerViewMessages.scrollToPosition(messages.size - 1)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendMessage(message: String) {
        thread {
            if (::writer.isInitialized) {
                writer.println(message)
                writer.flush()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        thread {
            try {
                if (::writer.isInitialized) {
                    writer.println(".q")
                    writer.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                isRunning = false
                if (::socket.isInitialized && !socket.isClosed) {
                    socket.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
