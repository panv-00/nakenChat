package com.example.nakenchat
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess

class GreetingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greeting)

        val editUsername = findViewById<EditText>(R.id.editTextUsername)
        val editPassword = findViewById<EditText>(R.id.editTextPassword)
        val editServer = findViewById<EditText>(R.id.editTextServer)
        val editPort = findViewById<EditText>(R.id.editTextPort)
        val buttonConnect = findViewById<Button>(R.id.buttonConnect)
        val buttonExit = findViewById<Button>(R.id.buttonExit)

        buttonConnect.setOnClickListener {
            val username = editUsername.text.toString()
            val password = editPassword.text.toString()
            val server = editServer.text.toString()
            val port = editPort.text.toString().toIntOrNull() ?: 6666

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("USERNAME", username)
                putExtra("PASSWORD", password)
                putExtra("SERVER", server)
                putExtra("PORT", port)
            }
            startActivity(intent)
        }

        buttonExit.setOnClickListener {
            finishAffinity()
            exitProcess(0)
        }
    }
}
