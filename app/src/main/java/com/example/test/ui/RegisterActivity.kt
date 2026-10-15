package com.example.test.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.test.data.RemoteApiManager
import com.example.test.data.UserRepository
import com.example.test.databinding.ActivityRegisterBinding
import com.example.test.util.AlertUtils
import com.example.test.util.NetworkSyncManager
import com.example.test.util.ValidationUtils

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupHintBehavior()
    }

    private fun setupListeners() {
        binding.btnRegistrar.setOnClickListener {
            val nombres = binding.etNombres.text.toString().trim()
            val apellidos = binding.etApellidos.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val clave = binding.etClave.text.toString()
            val repetirClave = binding.etRepetirClave.text.toString()

            // Requerimiento 5 y 8: Validar campos vacíos
            if (nombres.isEmpty() || apellidos.isEmpty() || email.isEmpty() || clave.isEmpty() || repetirClave.isEmpty()) {
                AlertUtils.showWarning(this, "Campos Obligatorios", "Todos los campos son obligatorios.")
                return@setOnClickListener
            }

            // Validar solo letras y espacios en nombres y apellidos
            if (!ValidationUtils.isOnlyLettersAndSpaces(nombres) || !ValidationUtils.isOnlyLettersAndSpaces(apellidos)) {
                AlertUtils.showWarning(
                    this,
                    "Formato Inválido",
                    "Nombres y apellidos solo deben contener letras y espacios."
                )
                return@setOnClickListener
            }

            // Validar formato de email
            if (!ValidationUtils.isValidEmail(email)) {
                AlertUtils.showWarning(
                    this,
                    "Formato Inválido",
                    "El formato del correo electrónico es inválido."
                )
                return@setOnClickListener
            }

            // Validar coincidencia de contraseña
            if (clave != repetirClave) {
                AlertUtils.showError(
                    this,
                    "Contraseñas No Coinciden",
                    "Las contraseñas ingresadas no son idénticas."
                )
                return@setOnClickListener
            }

            // Validar contraseña robusta
            if (!ValidationUtils.isPasswordStrong(clave)) {
                AlertUtils.showWarning(
                    this,
                    "Contraseña Débil",
                    "La contraseña debe tener mínimo 8 caracteres, al menos 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial."
                )
                return@setOnClickListener
            }

            if (NetworkSyncManager.isOnline) {
                // ─── ONLINE: MySQL es la fuente de verdad, no validar duplicado localmente ───
                val loading = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                    titleText = "Registrando..."
                    setCancelable(false)
                    show()
                }
                RemoteApiManager.registerUser(this, nombres, apellidos, email, clave) { result ->
                    loading.dismissWithAnimation()
                    when (result) {
                        is RemoteApiManager.RegisterResult.Success -> {
                            // Guardar también en SQLite local sincronizado con el ID de MySQL
                            UserRepository.addUser(nombres, apellidos, email, clave, isOffline = false, remoteId = result.id)
                            AlertUtils.showSuccess(this, "¡Registro Exitoso!",
                                "Usuario registrado en MySQL correctamente. Ahora puede iniciar sesión.") {
                                goToLogin()
                            }
                        }
                        is RemoteApiManager.RegisterResult.DuplicateEmail -> {
                            AlertUtils.showError(this, "Usuario Existente",
                                "Ya existe un usuario registrado con este correo electrónico en el servidor.")
                        }
                        is RemoteApiManager.RegisterResult.Error -> {
                            // Fallo de red → guardar offline
                            UserRepository.addUser(nombres, apellidos, email, clave, isOffline = true)
                            AlertUtils.showWarning(this, "Sin Conexión",
                                "No se pudo conectar al servidor. Registro guardado localmente y se sincronizará automáticamente.") {
                                goToLogin()
                            }
                        }
                    }
                }
            } else {
                // ─── OFFLINE: validar duplicado en SQLite local y guardar con PENDING_INSERT ──
                if (UserRepository.findUserByEmail(email) != null) {
                    AlertUtils.showError(
                        this,
                        "Usuario Existente",
                        "Ya existe un usuario registrado con este correo electrónico."
                    )
                    return@setOnClickListener
                }
                UserRepository.addUser(nombres, apellidos, email, clave, isOffline = true)
                AlertUtils.showSuccess(this, "¡Registro Guardado Localmente!",
                    "Sin conexión. El registro se sincronizará con MySQL automáticamente al reconectar.") {
                    goToLogin()
                }
            }
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun setupHintBehavior() {
        val fields = listOf(
            binding.etNombres      to "INGRESE NOMBRES",
            binding.etApellidos    to "INGRESE APELLIDOS",
            binding.etEmail        to "INGRESE E-MAIL",
            binding.etClave        to "INGRESE CLAVE",
            binding.etRepetirClave to "REPETIR CLAVE"
        )
        for ((field, hint) in fields) {
            field.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    field.hint = ""
                } else if (field.text.isNullOrEmpty()) {
                    field.hint = hint
                }
            }
        }
    }
}
