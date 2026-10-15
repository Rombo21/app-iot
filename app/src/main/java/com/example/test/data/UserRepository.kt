package com.example.test.data

import android.content.Context
import com.example.test.model.User
import kotlin.random.Random

object UserRepository {
    private var dbHelper: ConexionDbHelper? = null
    private val inMemoryUsers = mutableListOf<User>()
    private var nextId = 1

    fun init(context: Context) {
        dbHelper = ConexionDbHelper(context.applicationContext)
    }

    fun getAllUsers(): List<User> {
        val dbUsers = dbHelper?.obtenerUsuarios()
        return if (!dbUsers.isNullOrEmpty()) {
            dbUsers
        } else {
            inMemoryUsers.toList()
        }
    }

    fun addUser(nombres: String, apellidos: String, email: String, clave: String, isOffline: Boolean = false): User {
        val user = User(
            id = nextId++,
            nombres = nombres.trim(),
            apellidos = apellidos.trim(),
            email = email.trim(),
            clave = clave,
            isBlocked = false,
            syncStatus = if (isOffline) "PENDING_INSERT" else "SYNCED"
        )
        if (dbHelper != null) {
            val insertedId = dbHelper!!.insertarUsuario(user, pendingSync = isOffline)
            user.id = insertedId.toInt()
        } else {
            inMemoryUsers.add(user)
        }
        return user
    }

    fun findUserByEmail(email: String): User? {
        val dbUser = dbHelper?.obtenerUsuarioPorEmail(email.trim())
        if (dbUser != null) return dbUser
        return inMemoryUsers.find { it.email.equals(email.trim(), ignoreCase = true) }
    }

    fun getUserById(id: Int): User? {
        val dbUser = dbHelper?.obtenerUsuarioPorId(id)
        if (dbUser != null) return dbUser
        return inMemoryUsers.find { it.id == id }
    }

    sealed class AuthResult {
        data class Success(val user: User) : AuthResult()
        object Blocked : AuthResult()
        object InvalidCredentials : AuthResult()
    }

    fun authenticate(emailOrUser: String, clave: String): AuthResult {
        val query = emailOrUser.trim()
        val user = getAllUsers().find {
            (it.email.equals(query, ignoreCase = true) || 
             "${it.nombres} ${it.apellidos}".contains(query, ignoreCase = true)) && 
            it.clave == clave
        } ?: return AuthResult.InvalidCredentials

        if (user.isBlocked) {
            return AuthResult.Blocked
        }
        return AuthResult.Success(user)
    }

    // Compatibilidad con código previo
    fun validateCredentials(emailOrUser: String, clave: String): User? {
        val res = authenticate(emailOrUser, clave)
        return if (res is AuthResult.Success) res.user else null
    }

    fun updateUser(id: Int, nombres: String, apellidos: String, email: String, isOffline: Boolean = false): Boolean {
        if (dbHelper != null) {
            return dbHelper!!.actualizarUsuario(id, nombres, apellidos, email, isOffline) > 0
        }
        val index = inMemoryUsers.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = inMemoryUsers[index]
            inMemoryUsers[index] = current.copy(nombres = nombres, apellidos = apellidos, email = email)
            return true
        }
        return false
    }

    fun updatePassword(email: String, newPassword: String, isOffline: Boolean = false): Boolean {
        if (dbHelper != null) {
            val ok = dbHelper!!.actualizarClave(email, newPassword, isOffline)
            // Al guardar nueva contraseña, reactivar la cuenta
            dbHelper!!.setBloqueoCuenta(email, false)
            return ok
        }
        val user = findUserByEmail(email) ?: return false
        user.clave = newPassword
        user.isBlocked = false
        user.recoveryCode = null
        user.recoveryCodeExpiry = 0L
        return true
    }

    fun setBlocked(email: String, blocked: Boolean) {
        dbHelper?.setBloqueoCuenta(email, blocked)
        findUserByEmail(email)?.isBlocked = blocked
    }

    fun deleteUser(id: Int, isOffline: Boolean = false): Boolean {
        if (dbHelper != null) {
            return dbHelper!!.eliminarUsuario(id, isOffline)
        }
        return inMemoryUsers.removeAll { it.id == id }
    }

    // ==========================================
    // RECUPERACIÓN CON CÓDIGO DE 6 DÍGITOS Y 1 MINUTO
    // ==========================================

    fun generateRecoveryCode(email: String): String? {
        val user = findUserByEmail(email) ?: return null
        val code = String.format("%06d", Random.nextInt(100000, 1000000))
        user.recoveryCode = code
        user.recoveryCodeExpiry = System.currentTimeMillis() + 60000L // 60 segundos (1 minuto)

        // Al solicitar recuperación, la cuenta se bloquea temporalmente (Requerimiento Pauta)
        setBlocked(email, true)
        return code
    }

    fun getRemainingRecoverySeconds(email: String): Long {
        val user = findUserByEmail(email) ?: return 0L
        val remainingMs = user.recoveryCodeExpiry - System.currentTimeMillis()
        return if (remainingMs > 0) remainingMs / 1000L else 0L
    }

    sealed class CodeVerificationResult {
        object Valid : CodeVerificationResult()
        object Expired : CodeVerificationResult()
        object InvalidCode : CodeVerificationResult()
        object UserNotFound : CodeVerificationResult()
    }

    fun verifyRecoveryCode(email: String, enteredCode: String): CodeVerificationResult {
        val user = findUserByEmail(email) ?: return CodeVerificationResult.UserNotFound
        val now = System.currentTimeMillis()

        if (user.recoveryCode == null || user.recoveryCodeExpiry < now) {
            return CodeVerificationResult.Expired
        }
        if (user.recoveryCode != enteredCode.trim()) {
            return CodeVerificationResult.InvalidCode
        }
        return CodeVerificationResult.Valid
    }

    fun getPendingSyncUsers(): List<User> {
        return dbHelper?.obtenerCambiosPendientes() ?: emptyList()
    }

    fun markSynced(id: Int) {
        dbHelper?.marcarSincronizado(id)
    }
}
