package com.example.test.ui

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.test.R
import com.example.test.databinding.ActivitySensorsBinding
import com.example.test.util.AlertUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SensorsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySensorsBinding
    private lateinit var requestQueue: RequestQueue
    private val handler = Handler(Looper.getMainLooper())

    // Actuadores
    private var isBulbOn = false
    private var isTorchOn = false

    // Hardware: Linterna
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var hasFlashSupport = false

    // Intervalo de consulta (1 segundo como se solicita en el manual)
    private val REFRESH_INTERVAL_MS = 1000L

    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            fetchSensorData()
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySensorsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestQueue = Volley.newRequestQueue(this)

        initCameraManager()
        setupActuatorToggles()
        updateDateTime()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Apagar la linterna al salir de la pantalla para no dejarla encendida
        if (isTorchOn) {
            setTorchState(false, silent = true)
        }
    }

    private fun initCameraManager() {
        hasFlashSupport = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        if (hasFlashSupport) {
            try {
                cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cameraId = cameraManager?.cameraIdList?.firstOrNull()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        updateTorchUI(false)
        updateBulbUI(false)
    }

    private fun updateDateTime() {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.forLanguageTag("es-CL"))
        binding.tvSensorDateTime.text = sdf.format(calendar.time)
    }

    private fun fetchSensorData() {
        val url = "https://www.pnk.cl/muestra_datos.php"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val tempStr = response.optString("temperatura", "")
                    val humStr = response.optString("humedad", "")

                    if (tempStr.isNotEmpty()) {
                        binding.tvTempValue.text = "$tempStr °C"
                        val tempVal = tempStr.toFloatOrNull() ?: 0f

                        // Cambio dinámico de icono según umbral (>= 30 °C temperatura alta)
                        if (tempVal >= 30f) {
                            binding.ivSensorTemp.setImageResource(R.drawable.tempalta)
                            binding.tvTempValue.setTextColor(getColor(R.color.red))
                        } else {
                            binding.ivSensorTemp.setImageResource(R.drawable.tempbaja)
                            binding.tvTempValue.setTextColor(getColor(R.color.orange))
                        }
                    }

                    if (humStr.isNotEmpty()) {
                        binding.tvHumidityValue.text = "$humStr %"
                    }

                    binding.tvSensorStatus.text = "Conectado al servidor IoT (www.pnk.cl)"
                    binding.tvSensorStatus.setTextColor(getColor(R.color.primary_dark))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                // No botar la app en caso de fallo de red
                binding.tvSensorStatus.text = "Sin conexión con servidor IoT (reintentando...)"
                binding.tvSensorStatus.setTextColor(getColor(R.color.red))
            }
        )

        requestQueue.add(request)
    }

    private fun setupActuatorToggles() {
        // Toggle Ampolleta (Solo visual e interactivo)
        binding.btnToggleBulb.setOnClickListener {
            isBulbOn = !isBulbOn
            updateBulbUI(isBulbOn)
            if (isBulbOn) {
                AlertUtils.showSuccess(this, "Actuador Visual", "Ampolleta simulada ENCENDIDA")
            } else {
                AlertUtils.showWarning(this, "Actuador Visual", "Ampolleta simulada APAGADA")
            }
        }

        // Toggle Linterna (Control real de hardware con CameraManager)
        binding.btnToggleTorch.setOnClickListener {
            val newState = !isTorchOn
            setTorchState(newState)
        }
    }

    private fun setTorchState(enable: Boolean, silent: Boolean = false) {
        if (hasFlashSupport && cameraManager != null && cameraId != null) {
            try {
                cameraManager?.setTorchMode(cameraId!!, enable)
                isTorchOn = enable
                updateTorchUI(isTorchOn)

                if (!silent) {
                    if (enable) {
                        AlertUtils.showSuccess(this, "Hardware Linterna", "Linterna del dispositivo ENCENDIDA")
                    } else {
                        AlertUtils.showWarning(this, "Hardware Linterna", "Linterna del dispositivo APAGADA")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Si ocurre una excepción de cámara, actualizar solo UI y notificar
                isTorchOn = enable
                updateTorchUI(isTorchOn)
                if (!silent) {
                    AlertUtils.showError(this, "Linterna", "Error al acceder al flash: ${e.message}")
                }
            }
        } else {
            // Emulador o dispositivo sin flash físico
            isTorchOn = enable
            updateTorchUI(isTorchOn)
            if (!silent) {
                if (enable) {
                    AlertUtils.showSuccess(this, "Linterna", "Linterna activada (modo simulado)")
                } else {
                    AlertUtils.showWarning(this, "Linterna", "Linterna desactivada")
                }
            }
        }
    }

    private fun updateTorchUI(enabled: Boolean) {
        if (enabled) {
            binding.ivTorch.setImageResource(R.drawable.ic_torch_on)
            binding.tvTorchState.text = "ENCENDIDA"
            binding.tvTorchState.setTextColor(getColor(R.color.primary_dark))
        } else {
            binding.ivTorch.setImageResource(R.drawable.ic_torch_off)
            binding.tvTorchState.text = "APAGADA"
            binding.tvTorchState.setTextColor(getColor(R.color.text_muted))
        }
    }

    private fun updateBulbUI(enabled: Boolean) {
        if (enabled) {
            binding.ivBulb.setImageResource(R.drawable.ic_bulb_on)
            binding.tvBulbState.text = "ENCENDIDA"
            binding.tvBulbState.setTextColor(getColor(R.color.primary_dark))
        } else {
            binding.ivBulb.setImageResource(R.drawable.ic_bulb_off)
            binding.tvBulbState.text = "APAGADA"
            binding.tvBulbState.setTextColor(getColor(R.color.text_muted))
        }
    }
}
