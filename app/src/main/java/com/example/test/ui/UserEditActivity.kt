package com.example.test.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.test.data.RemoteApiManager
import com.example.test.data.UserRepository
import com.example.test.databinding.ActivityUserEditBinding
import com.example.test.model.User
import com.example.test.util.AlertUtils
import com.example.test.util.NetworkSyncManager
import com.example.test.util.SessionManager
import com.example.test.util.ValidationUtils

class UserEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserEditBinding
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId    = intent.getIntExtra("user_id", -1)
        val nombres   = intent.getStringExtra("user_nombres") ?: ""
        val apellidos = intent.getStringExtra("user_apellidos") ?: ""
        val email     = intent.getStringExtra("user_email") ?: ""
        val clave     = intent.getStringExtra("user_clave") ?: ""
        val isBlocked = intent.getBooleanExtra("user_is_blocked", false)

        currentUser = if (nombres.isNotEmpty() || email.isNotEmpty()) {
            User(userId, nombres, apellidos, email, clave, isBlocked)
        } else {
            UserRepository.getUserById(userId)
        }

        if (currentUser == null) {
            AlertUtils.showError(this, "Error", "No se encontró el usuario seleccionado.") {
                finish()
            }
            return
        }

        populateFields()
        setupListeners()
    }

    private fun populateFields() {
        currentUser?.let { user ->
            binding.etEditNombres.setText(user.nombres)
            binding.etEditApellidos.setText(user.apellidos)
            binding.etEditEmail.setText(user.email)
        }
    }

    private fun setupListeners() {
        binding.btnModificar.setOnClickListener {
            val nombres   = binding.etEditNombres.text.toString().trim()
            val apellidos = binding.etEditApellidos.text.toString().trim()
            val email     = binding.etEditEmail.text.toString().trim()

            // Validaciones
            if (nombres.isEmpty() || apellidos.isEmpty() || email.isEmpty()) {
                AlertUtils.showWarning(this, "Campos Obligatorios", "Ningún campo puede quedar vacío.")
                return@setOnClickListener
            }
            if (!ValidationUtils.isOnlyLettersAndSpaces(nombres) || !ValidationUtils.isOnlyLettersAndSpaces(apellidos)) {
                AlertUtils.showWarning(this, "Formato Inválido",
                    "Nombres y apellidos solo pueden contener letras y espacios.")
                return@setOnClickListener
            }
            if (!ValidationUtils.isValidEmail(email)) {
                AlertUtils.showWarning(this, "Formato Inválido",
                    "Ingrese un formato de correo electrónico válido.")
                return@setOnClickListener
            }

            currentUser?.let { user ->
                if (NetworkSyncManager.isOnline) {
                    // ─── ONLINE: actualizar en MySQL ─────────────────────
                    val loading = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                        titleText = "Actualizando..."
                        setCancelable(false)
                        show()
                    }
                    RemoteApiManager.updateUser(this, user.id, nombres, apellidos, email) { result ->
                        loading.dismissWithAnimation()
                        when (result) {
                            is RemoteApiManager.UpdateResult.Success -> {
                                // Actualizar también en SQLite local
                                UserRepository.updateUser(user.id, nombres, apellidos, email, isOffline = false)
                                AlertUtils.showSuccess(this, "¡Actualizado!",
                                    "Datos del usuario actualizados en MySQL correctamente.") { finish() }
                            }
                            is RemoteApiManager.UpdateResult.DuplicateEmail -> {
                                AlertUtils.showError(this, "Correo Duplicado",
                                    "Ese correo ya está asignado a otro usuario en la base de datos.")
                            }
                            is RemoteApiManager.UpdateResult.Error -> {
                                // Fallo de red → guardar en SQLite pendiente
                                UserRepository.updateUser(user.id, nombres, apellidos, email, isOffline = true)
                                AlertUtils.showWarning(this, "Sin Conexión",
                                    "No se pudo conectar al servidor. Cambios guardados localmente y se sincronizarán al recuperar internet.") { finish() }
                            }
                        }
                    }
                } else {
                    // ─── OFFLINE: guardar en SQLite con PENDING_UPDATE ────
                    UserRepository.updateUser(user.id, nombres, apellidos, email, isOffline = true)
                    AlertUtils.showSuccess(this, "¡Guardado Localmente!",
                        "Sin conexión a internet. Los cambios han sido guardados en SQLite y se sincronizarán automáticamente.") { finish() }
                }
            }
        }

        binding.btnEliminar.setOnClickListener {
            currentUser?.let { user ->
                // — Proteger auto-eliminación: no se puede eliminar al usuario de la sesión activa
                val sessionEmail = SessionManager.getEmail(this)
                if (!sessionEmail.isNullOrEmpty() && sessionEmail.equals(user.email, ignoreCase = true)) {
                    AlertUtils.showError(
                        this,
                        "Acción No Permitida",
                        "No puedes eliminar tu propia cuenta mientras tienes la sesión activa."
                    )
                    return@setOnClickListener
                }

                AlertUtils.showConfirmation(this, "¿Estás seguro?",
                    "Esta acción eliminará al usuario permanentemente.") {

                    if (NetworkSyncManager.isOnline) {
                        // ─── ONLINE: eliminar en MySQL ─────────────────────
                        val loading = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                            titleText = "Eliminando..."
                            setCancelable(false)
                            show()
                        }
                        RemoteApiManager.deleteUser(this, user.id, user.email) { success, errorMsg ->
                            loading.dismissWithAnimation()
                            if (success) {
                                // Eliminar también de SQLite local
                                UserRepository.deleteUser(user.id, user.email, isOffline = false)
                                AlertUtils.showSuccess(this, "¡Eliminado!",
                                    "Usuario eliminado de MySQL correctamente.") { finish() }
                            } else {
                                // Fallo → marcar para eliminar al reconectar
                                UserRepository.deleteUser(user.id, user.email, isOffline = true)
                                AlertUtils.showWarning(this, "Sin Conexión",
                                    "No se pudo conectar. Usuario marcado para eliminar al reconectar.") { finish() }
                            }
                        }
                    } else {
                        // ─── OFFLINE: soft delete en SQLite ────────────────
                        UserRepository.deleteUser(user.id, user.email, isOffline = true)
                        AlertUtils.showSuccess(this, "¡Marcado para Eliminar!",
                            "Se eliminará de MySQL automáticamente al recuperar la conexión.") { finish() }
                    }
                }
            }
        }
    }
}
