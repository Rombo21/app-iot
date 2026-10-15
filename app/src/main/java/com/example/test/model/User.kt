package com.example.test.model

data class User(
    var id: Int,
    var nombres: String,
    var apellidos: String,
    var email: String,
    var clave: String,
    var isBlocked: Boolean = false,
    var syncStatus: String = "SYNCED", // SYNCED, PENDING_INSERT, PENDING_UPDATE, PENDING_DELETE
    var recoveryCode: String? = null,
    var recoveryCodeExpiry: Long = 0L
)
