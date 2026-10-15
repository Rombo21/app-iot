package com.example.test.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.test.databinding.ActivityDeveloperInfoBinding
import com.example.test.util.AlertUtils

class DeveloperInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeveloperInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupExternalLinks()
    }

    private fun setupExternalLinks() {
        // Enlace a GitHub
        binding.btnGithub.setOnClickListener {
            openUrl("https://github.com")
        }

        // Enlace a LinkedIn
        binding.btnLinkedin.setOnClickListener {
            openUrl("https://linkedin.com")
        }

        // Enlace a Portafolio
        binding.btnPortfolio.setOnClickListener {
            openUrl("https://github.com")
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            AlertUtils.showError(this, "Navegador", "No se pudo abrir el enlace web solicitado.")
        }
    }
}
