package com.example.test.ui

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.test.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pantalla completa (complementos2.md)
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Arrancar animación Lottie
        binding.lottieAnimationView.setFailureListener { exception ->
            android.util.Log.e("SplashActivity", "Error Lottie: ${exception.message}", exception)
        }
        binding.lottieAnimationView.repeatCount = com.airbnb.lottie.LottieDrawable.INFINITE
        binding.lottieAnimationView.playAnimation()

        // Animación del porcentaje: 0% → 100% durante exactamente 7 segundos
        val totalDurationMs = 7000L
        val animator = ValueAnimator.ofInt(0, 100).apply {
            duration = totalDurationMs
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val progress = anim.animatedValue as Int
                binding.progressBarSplash.progress = progress
                binding.tvLoadingPercent.text = "$progress%"
            }
        }
        animator.start()

        // Navegar a Login exactamente a los 7 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, totalDurationMs)
    }
}
