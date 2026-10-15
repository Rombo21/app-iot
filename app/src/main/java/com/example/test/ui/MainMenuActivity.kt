package com.example.test.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.test.databinding.ActivityMainMenuBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainMenuBinding
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateTimeRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateTimeRunnable)
    }

    private fun updateDateTime() {
        // Formato idéntico al mockup: 16-10-2025, 20:25:30
        val sdf = SimpleDateFormat("dd-MM-yyyy, HH:mm:ss", Locale.getDefault())
        binding.tvDateTime.text = "Fecha/Hora:${sdf.format(Date())}"
    }

    private fun setupListeners() {
        binding.btnMenuCrud.setOnClickListener {
            val intent = Intent(this, UserManagementMenuActivity::class.java)
            startActivity(intent)
        }

        binding.btnMenuSensores.setOnClickListener {
            val intent = Intent(this, SensorsActivity::class.java)
            startActivity(intent)
        }

        binding.btnMenuDesarrollador.setOnClickListener {
            val intent = Intent(this, DeveloperInfoActivity::class.java)
            startActivity(intent)
        }
    }
}
