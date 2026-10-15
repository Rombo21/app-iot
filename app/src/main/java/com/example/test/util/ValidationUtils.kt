package com.example.test.util

import android.util.Patterns
import java.text.Normalizer

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    /**
     * Requerimiento 3, 4 y 5:
     * ≥8 caracteres, mínimo 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial
     */
    fun isPasswordStrong(password: String): Boolean {
        if (password.length < 8) return false
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasUpper && hasLower && hasDigit && hasSpecial
    }

    /**
     * Nombres y Apellidos: solo letras y espacios
     */
    fun isOnlyLettersAndSpaces(text: String): Boolean {
        if (text.isBlank()) return false
        val regex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$")
        return text.matches(regex)
    }

    fun removeAccents(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}
