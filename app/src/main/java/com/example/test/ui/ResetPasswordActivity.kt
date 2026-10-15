package com.example.test.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.test.data.RemoteApiManager
import com.example.test.data.UserRepository
import com.example.test.databinding.ActivityResetPasswordBinding
import com.example.test.util.AlertUtils
import com.example.test.util.NetworkSyncManager
import com.example.test.util.ValidationUtils

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        email = intent.getStringExtra("email") ?: ""

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnCrear.setOnClickListener {
            val pass1 = binding.etNewPassword.text.toString()
            val pass2 = binding.etRepeatPassword.text.toString()

            // Validación de campos vacíos
            if (pass1.isEmpty() || pass2.isEmpty()) {
                AlertUtils.showWarning(this, "Campos Obligatorios", "Complete ambos campos de contraseña.")
                return@setOnClickListener
            }

            // Validación de coincidencia
            if (pass1 != pass2) {
                AlertUtils.showError(this, "No Coinciden", "Las contraseñas ingresadas no coinciden.")
                return@setOnClickListener
            }

            // Requerimiento 3 y 4: Robustez (≥8 car., 1 mayús, 1 minús, 1 núm, 1 esp)
            if (!ValidationUtils.isPasswordStrong(pass1)) {
                AlertUtils.showWarning(
                    this,
                    "Contraseña Débil",
                    "La contraseña debe tener mínimo 8 caracteres, al menos 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial."
                )
                return@setOnClickListener
            }

            fun finishReset(isOffline: Boolean) {
                UserRepository.updatePassword(email, pass1, isOffline = isOffline)

                val msg = if (isOffline) {
                    "Contraseña actualizada en base de datos local y cuenta reactivada.\nSe sincronizará con MySQL en la nube al recuperar la conexión."
                } else {
                    "Contraseña actualizada exitosamente y cuenta reactivada. Ahora puede iniciar sesión con su nueva clave."
                }

                AlertUtils.showSuccess(this, "¡Éxito!", msg) {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }

            if (NetworkSyncManager.isOnline) {
                val loading = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                    titleText = "Actualizando clave..."
                    setCancelable(false)
                    show()
                }
                RemoteApiManager.resetPassword(this, email, pass1) { success, _ ->
                    loading.dismissWithAnimation()
                    finishReset(isOffline = !success)
                }
            } else {
                finishReset(isOffline = true)
            }
        }
    }
}
