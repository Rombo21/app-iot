package com.example.test.data

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.test.model.User
import org.json.JSONObject

/**
 * Centralizador de llamadas HTTP a la API PHP en AWS.
 * Todos los endpoints usan el mismo archivo usuarios_api.php con parametro ?action=...
 *
 * Base URL: http://52.2.255.205/  (ajustar segun IP/dominio real del servidor AWS)
 */
object RemoteApiManager {

    // -------------------------------------------------------
    const val BASE_URL = "http://100.62.185.79/"
    private const val API_USUARIOS = "${BASE_URL}usuarios_api.php"
    private const val API_LOGIN    = "${BASE_URL}apiconsultausu.php"
    private const val API_PHPMAILER = "${BASE_URL}enviar_codigo_phpmailer.php"
    const val API_SYNC     = "${BASE_URL}sync_usuario.php"

    // -------------------------------------------------------
    // LOGIN: Autenticar credenciales contra MySQL
    // -------------------------------------------------------
    sealed class LoginResult {
        data class Success(val id: Int, val nombre: String, val apellido: String) : LoginResult()
        object Blocked   : LoginResult()
        object InvalidCredentials : LoginResult()
        data class NetworkError(val msg: String) : LoginResult()
    }

    fun login(
        context: Context,
        emailOrUser: String,
        clave: String,
        onResult: (LoginResult) -> Unit
    ) {
        val url = "$API_LOGIN?usu=${encode(emailOrUser)}&pass=${encode(clave)}"
        val queue = Volley.newRequestQueue(context)

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                when (response.optString("estado")) {
                    "1"        -> onResult(LoginResult.Success(
                        id       = response.optInt("id"),
                        nombre   = response.optString("nombre"),
                        apellido = response.optString("apellido")
                    ))
                    "bloqueado" -> onResult(LoginResult.Blocked)
                    else        -> onResult(LoginResult.InvalidCredentials)
                }
            },
            { error -> onResult(LoginResult.NetworkError(error.message ?: "Error de red")) }
        )
        queue.add(request)
    }

    // -------------------------------------------------------
    // LISTAR usuarios desde MySQL
    // -------------------------------------------------------
    fun listUsers(
        context: Context,
        onSuccess: (List<User>) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$API_USUARIOS?action=list_users"
        val queue = Volley.newRequestQueue(context)

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                if (response.optBoolean("success")) {
                    val arr = response.optJSONArray("usuarios") ?: run {
                        onSuccess(emptyList()); return@JsonObjectRequest
                    }
                    val list = mutableListOf<User>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(User(
                            id        = obj.optInt("id"),
                            nombres   = obj.optString("nombre"),
                            apellidos = obj.optString("apellido"),
                            email     = obj.optString("usuario"),
                            clave     = "",
                            isBlocked = obj.optInt("bloqueado") == 1
                        ))
                    }
                    onSuccess(list)
                } else {
                    onError(response.optString("error", "Error al obtener usuarios"))
                }
            },
            { error -> onError(error.message ?: "Error de red") }
        )
        queue.add(request)
    }

    // -------------------------------------------------------
    // REGISTRAR usuario en MySQL
    // -------------------------------------------------------
    sealed class RegisterResult {
        data class Success(val id: Int) : RegisterResult()
        object DuplicateEmail : RegisterResult()
        data class Error(val msg: String) : RegisterResult()
    }

    fun registerUser(
        context: Context,
        nombres: String,
        apellidos: String,
        email: String,
        clave: String,
        onResult: (RegisterResult) -> Unit
    ) {
        val queue = Volley.newRequestQueue(context)
        val request = object : StringRequest(Method.POST, "$API_USUARIOS?action=register",
            { resp ->
                val json = JSONObject(resp)
                when {
                    json.optBoolean("success") -> onResult(RegisterResult.Success(json.optInt("id")))
                    json.optString("error") == "duplicate_email" -> onResult(RegisterResult.DuplicateEmail)
                    else -> onResult(RegisterResult.Error(json.optString("error", "Error desconocido")))
                }
            },
            { error -> onResult(RegisterResult.Error(error.message ?: "Error de red")) }
        ) {
            override fun getParams() = mutableMapOf(
                "nombres"   to nombres,
                "apellidos" to apellidos,
                "email"     to email,
                "clave"     to clave
            )
        }
        queue.add(request)
    }

    // -------------------------------------------------------
    // ACTUALIZAR usuario en MySQL
    // -------------------------------------------------------
    sealed class UpdateResult {
        object Success : UpdateResult()
        object DuplicateEmail : UpdateResult()
        data class Error(val msg: String) : UpdateResult()
    }

    fun updateUser(
        context: Context,
        id: Int,
        nombres: String,
        apellidos: String,
        email: String,
        onResult: (UpdateResult) -> Unit
    ) {
        val queue = Volley.newRequestQueue(context)
        val request = object : StringRequest(Method.POST, "$API_USUARIOS?action=update",
            { resp ->
                val json = JSONObject(resp)
                when {
                    json.optBoolean("success") -> onResult(UpdateResult.Success)
                    json.optString("error") == "duplicate_email" -> onResult(UpdateResult.DuplicateEmail)
                    else -> onResult(UpdateResult.Error(json.optString("error", "Error desconocido")))
                }
            },
            { error -> onResult(UpdateResult.Error(error.message ?: "Error de red")) }
        ) {
            override fun getParams() = mutableMapOf(
                "id"        to id.toString(),
                "nombres"   to nombres,
                "apellidos" to apellidos,
                "email"     to email
            )
        }
        queue.add(request)
    }

    // -------------------------------------------------------
    // ELIMINAR usuario en MySQL
    // -------------------------------------------------------
    fun deleteUser(
        context: Context,
        id: Int,
        email: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        val queue = Volley.newRequestQueue(context)
        val request = object : StringRequest(Method.POST, "$API_USUARIOS?action=delete",
            { resp ->
                val json = JSONObject(resp)
                onResult(json.optBoolean("success"), json.optString("error", ""))
            },
            { error -> onResult(false, error.message ?: "Error de red") }
        ) {
            override fun getParams() = mutableMapOf(
                "id"    to id.toString(),
                "email" to email
            )
        }
        queue.add(request)
    }

    // -------------------------------------------------------
    // ENVIAR CODIGO PHPMailer para recuperacion de contrasena
    // -------------------------------------------------------
    fun sendRecoveryCode(
        context: Context,
        email: String,
        onResult: (Boolean, String, String?) -> Unit  // (success, mensaje/error, codigoDebug?)
    ) {
        val queue = Volley.newRequestQueue(context)
        val request = object : StringRequest(Method.POST, API_PHPMAILER,
            { resp ->
                val json = JSONObject(resp)
                val ok  = json.optBoolean("success")
                val msg = if (ok) json.optString("mensaje") else json.optString("error")
                val dbg = if (json.has("codigo_debug")) json.getString("codigo_debug") else null
                onResult(ok, msg, dbg)
            },
            { error -> onResult(false, error.message ?: "Error de red", null) }
        ) {
            override fun getParams() = mutableMapOf("email" to email)
        }
        queue.add(request)
    }

    // -------------------------------------------------------
    // RESTABLECER CONTRASEÑA Y DESBLOQUEAR EN MYSQL
    // -------------------------------------------------------
    fun resetPassword(
        context: Context,
        email: String,
        newPass: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val queue = Volley.newRequestQueue(context)
        val request = object : StringRequest(Method.POST, "$API_USUARIOS?action=reset_password",
            { resp ->
                val json = JSONObject(resp)
                onResult(json.optBoolean("success"), json.optString("error", ""))
            },
            { error -> onResult(false, error.message ?: "Error de red") }
        ) {
            override fun getParams() = mutableMapOf(
                "email" to email,
                "clave" to newPass
            )
        }
        queue.add(request)
    }

    // -------------------------------------------------------
    // Helper: encode basico para URL (solo espacios)
    // -------------------------------------------------------
    private fun encode(s: String): String = s.replace(" ", "%20").replace("@", "%40")
}
