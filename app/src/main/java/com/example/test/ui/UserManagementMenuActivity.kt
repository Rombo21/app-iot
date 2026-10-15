package com.example.test.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.test.databinding.ActivityUserManagementMenuBinding

class UserManagementMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManagementMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        // Requerimiento 7: “Ingresar usuario” (abre el formulario de registro)
        binding.btnIngresarUsuarios.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Requerimiento 7: “Listar usuarios” (muestra la tabla de usuarios)
        binding.btnListarUsuarios.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            startActivity(intent)
        }
    }
}
