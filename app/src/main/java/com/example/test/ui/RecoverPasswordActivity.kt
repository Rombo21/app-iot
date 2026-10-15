package com.example.test.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.test.data.RemoteApiManager
import com.example.test.data.UserRepository
import com.example.test.databinding.ActivityRecoverPasswordBinding
import com.example.test.util.AlertUtils
import com.example.test.util.NetworkSyncManager
import com.example.test.util.ValidationUtils

class RecoverPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecoverPasswordBinding
    private var timer: CountDownTimer? = null
    private var currentTargetEmail: String? = null
    private var generatedCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecoverPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCodeBoxes()
        setupListeners()
        setupHintBehavior()
    }

    private fun setupCodeBoxes() {
        val boxes = listOf(
            binding.code1,
            binding.code2,
            binding.code3,
            binding.code4,
            binding.code5,
            binding.code6
        )
        for (i in boxes.indices) {
            val current = boxes[i]
            val next = if (i < boxes.size - 1) boxes[i + 1] else null
            val prev = if (i > 0) boxes[i - 1] else null

            current.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        next?.requestFocus()
                    } else if (s?.length == 0) {
                        prev?.requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun startTimer() {
        timer?.cancel()
        // Vigencia estricta de 60 segundos (1 minuto) según requerimiento de la pauta
        timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.tvTimer.text = "$seconds Segundos restantes"
                binding.tvTimer.setTextColor(getColor(com.example.test.R.color.primary_dark))
            }
            override fun onFinish() {
                binding.tvTimer.text = "Código expirado (Vigencia 1 min superada)"
                binding.tvTimer.setTextColor(getColor(com.example.test.R.color.red))
                AlertUtils.showWarning(
                    this@RecoverPasswordActivity,
                    "Código Vencido",
                    "El código de verificación ha vencido (1 minuto). Por favor solicite uno nuevo."
                )
            }
        }.start()
    }

    private fun setupListeners() {
        binding.btnRecuperar.setOnClickListener {
            val email = binding.etEmailRecover.text.toString().trim()
            if (email.isEmpty()) {
                AlertUtils.showWarning(this, "Campo Vacío", "Por favor ingrese su correo electrónico.")
                return@setOnClickListener
            }
            if (!ValidationUtils.isValidEmail(email)) {
                AlertUtils.showWarning(this, "Formato Inválido", "Ingrese un correo electrónico válido.")
                return@setOnClickListener
            }

            // Verificar si el usuario existe en SQLite o memoria
            val user = UserRepository.findUserByEmail(email)
            if (user == null) {
                AlertUtils.showError(
                    this,
                    "Usuario no encontrado",
                    "El correo $email no se encuentra registrado en el sistema."
                )
                return@setOnClickListener
            }

            currentTargetEmail = email

            if (NetworkSyncManager.isOnline) {
                // ─── ONLINE: enviar código real via PHPMailer en AWS ────────────
                val loading = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                    titleText = "Enviando código..."
                    setCancelable(false)
                    show()
                }
                RemoteApiManager.sendRecoveryCode(this, email) { success, msg, codigoDebug ->
                    loading.dismissWithAnimation()
                    if (success) {
                        val finalCode = codigoDebug ?: UserRepository.generateRecoveryCode(email) ?: "123456"
                        UserRepository.storeRecoveryCode(email, finalCode)
                        generatedCode = finalCode
                        limpiarCasillas()
                        startTimer()
                        AlertUtils.showSuccess(this, "Código Enviado",
                            "Un código de 6 dígitos fue enviado a: $email\n\n⚠️ Vigencia estricta de 1 minuto. Su cuenta está bloqueada temporalmente.")
                    } else {
                        // Error en PHPMailer → fallback a generación local
                        val code = UserRepository.generateRecoveryCode(email) ?: "123456"
                        UserRepository.storeRecoveryCode(email, code)
                        generatedCode = code
                        limpiarCasillas()
                        startTimer()
                        AlertUtils.showWarning(this, "PHPMailer no disponible",
                            "No se pudo enviar el email (${msg}). Código de recuperación generado localmente: $code\n\nVigencia: 1 minuto.")
                    }
                }
            } else {
                // ─── OFFLINE: generación local (SQLite) ─────────────────────────
                val code = UserRepository.generateRecoveryCode(email) ?: "123456"
                UserRepository.storeRecoveryCode(email, code)
                generatedCode = code
                limpiarCasillas()
                startTimer()
                AlertUtils.showSuccess(this, "Código Generado (Sin Internet)",
                    "Sin conexión a internet. Código local generado: $code\n(Normalmente se envía por correo vía PHPMailer)\n\n⚠️ Vigencia: 1 minuto.")
            }
        }

        binding.btnValidarCodigo.setOnClickListener {
            val email = currentTargetEmail ?: binding.etEmailRecover.text.toString().trim()
            if (email.isEmpty() || generatedCode == null) {
                AlertUtils.showWarning(this, "Email Requerido", "Ingrese su email y presione RECUPERAR para generar el código.")
                return@setOnClickListener
            }

            val enteredCode = "${binding.code1.text}${binding.code2.text}${binding.code3.text}${binding.code4.text}${binding.code5.text}${binding.code6.text}"
            if (enteredCode.length < 6) {
                AlertUtils.showWarning(this, "Código Incompleto", "Debe ingresar el código completo de 6 dígitos.")
                return@setOnClickListener
            }

            // Validar código y tiempo de vigencia en UserRepository
            when (UserRepository.verifyRecoveryCode(email, enteredCode)) {
                UserRepository.CodeVerificationResult.Valid -> {
                    timer?.cancel()
                    val intent = Intent(this, ResetPasswordActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                }
                UserRepository.CodeVerificationResult.Expired -> {
                    AlertUtils.showWarning(
                        this,
                        "Código Expirado",
                        "El plazo de 1 minuto ha concluido. El código ya no es válido, solicite uno nuevo."
                    )
                }
                UserRepository.CodeVerificationResult.InvalidCode -> {
                    AlertUtils.showError(
                        this,
                        "Código Incorrecto",
                        "El código ingresado no coincide con el enviado a su correo."
                    )
                }
                UserRepository.CodeVerificationResult.UserNotFound -> {
                    AlertUtils.showError(this, "Error", "Usuario no encontrado.")
                }
            }
        }
    }

    private fun limpiarCasillas() {
        listOf(binding.code1, binding.code2, binding.code3, binding.code4,
               binding.code5, binding.code6).forEach { it.text?.clear() }
    }

    private fun setupHintBehavior() {
        val hintEmail = binding.etEmailRecover.hint?.toString() ?: ""
        binding.etEmailRecover.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.etEmailRecover.hint = ""
            } else if (binding.etEmailRecover.text.isNullOrEmpty()) {
                binding.etEmailRecover.hint = hintEmail
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
