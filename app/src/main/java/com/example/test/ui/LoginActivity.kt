package com.example.test.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.test.data.RemoteApiManager
import com.example.test.data.UserRepository
import com.example.test.databinding.ActivityLoginBinding
import com.example.test.util.AlertUtils
import com.example.test.util.NetworkSyncManager
import com.example.test.util.SessionManager
import com.example.test.util.ValidationUtils

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupHintBehavior()
    }

    private fun setupListeners() {
        binding.btnIngresar.setOnClickListener {
            val userOrEmail = binding.etUserEmail.text.toString().trim()
            val password    = binding.etPassword.text.toString()

            // Validar campos en blanco
            if (userOrEmail.isEmpty() || password.isEmpty()) {
                AlertUtils.showWarning(this, "Campos Obligatorios",
                    "Por favor, complete tanto el usuario/email como la contraseña.")
                return@setOnClickListener
            }

            // Si contiene '@', validar formato de email
            if (userOrEmail.contains("@") && !ValidationUtils.isValidEmail(userOrEmail)) {
                AlertUtils.showWarning(this, "Formato Inválido",
                    "El formato del correo electrónico ingresado no es válido.")
                return@setOnClickListener
            }

            if (NetworkSyncManager.isOnline) {
                // ─────── MODO ONLINE: autenticar contra MySQL en AWS ────────
                val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                    titleText = "Verificando..."
                    setCancelable(false)
                    show()
                }

                RemoteApiManager.login(this, userOrEmail, password) { result ->
                    loadingDialog.dismissWithAnimation()
                    when (result) {
                        is RemoteApiManager.LoginResult.Success -> {
                            val userEmail = if (result.email.isNotEmpty()) result.email else (if (userOrEmail.contains("@")) userOrEmail else "")
                            val finalEmail = if (userEmail.isNotEmpty()) userEmail else userOrEmail

                            // Guardar / actualizar de inmediato las credenciales en SQLite local para permitir acceso offline
                            UserRepository.saveOrUpdateLocalUser(
                                id = result.id,
                                nombres = result.nombre,
                                apellidos = result.apellido,
                                email = finalEmail,
                                clave = password
                            )

                            // Guardar sesión: email + nombre del usuario logueado
                            SessionManager.saveSession(this, finalEmail, result.nombre)
                            goToMainMenu()
                        }
                        is RemoteApiManager.LoginResult.Blocked -> {
                            AlertUtils.showError(this, "Cuenta Bloqueada",
                                "Esta cuenta está bloqueada temporalmente por recuperación de contraseña.\n\nValide el código de 6 dígitos enviado a su correo para reactivarla.")
                        }
                        is RemoteApiManager.LoginResult.InvalidCredentials -> {
                            AlertUtils.showError(this, "Error de Autenticación",
                                "Usuario/Email o contraseña incorrectos. Verifique sus datos o use la opción de recuperar contraseña.")
                        }
                        is RemoteApiManager.LoginResult.NetworkError -> {
                            // Si la API falla, caer a SQLite local
                            authenticateLocally(userOrEmail, password)
                        }
                    }
                }
            } else {
                // ─────── MODO OFFLINE: autenticar contra SQLite local ────────
                authenticateLocally(userOrEmail, password)
            }
        }

        binding.tvRegistrarme.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvOlvideClave.setOnClickListener {
            startActivity(Intent(this, RecoverPasswordActivity::class.java))
        }
    }

    /** Autenticación local contra SQLite cuando no hay red */
    private fun authenticateLocally(userOrEmail: String, password: String) {
        when (val auth = UserRepository.authenticate(userOrEmail, password)) {
            is UserRepository.AuthResult.Success -> {
                val user = auth.user
                SessionManager.saveSession(this, user.email, user.nombres)
                goToMainMenu()
            }
            is UserRepository.AuthResult.Blocked          -> {
                AlertUtils.showError(this, "Cuenta Bloqueada",
                    "Esta cuenta ha sido bloqueada temporalmente debido a una solicitud de recuperación de contraseña.\n\nPor favor, valide el código de 6 dígitos enviado a su correo para reactivarla.")
            }
            is UserRepository.AuthResult.InvalidCredentials -> {
                val suffix = if (!NetworkSyncManager.isOnline) "\n\n(Sin conexión: verificando en base de datos local)" else ""
                AlertUtils.showError(this, "Error de Autenticación",
                    "Usuario/Email o contraseña incorrectos. Verifique sus datos o use la opción de recuperar contraseña.$suffix")
            }
        }
    }

    private fun goToMainMenu() {
        startActivity(Intent(this, MainMenuActivity::class.java))
        finish()
    }

    private fun setupHintBehavior() {
        val hintEmail = binding.etUserEmail.hint?.toString() ?: ""
        binding.etUserEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.etUserEmail.hint = ""
            else if (binding.etUserEmail.text.isNullOrEmpty()) binding.etUserEmail.hint = hintEmail
        }

        val hintPass = binding.etPassword.hint?.toString() ?: ""
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.etPassword.hint = ""
            else if (binding.etPassword.text.isNullOrEmpty()) binding.etPassword.hint = hintPass
        }
    }
}
