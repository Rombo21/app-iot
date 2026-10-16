package com.example.test.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.test.model.User

class ConexionDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSql = """
            CREATE TABLE $TABLE_USUARIOS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT NOT NULL,
                $COL_APELLIDO TEXT NOT NULL,
                $COL_EMAIL TEXT NOT NULL UNIQUE,
                $COL_CLAVE TEXT NOT NULL,
                $COL_BLOQUEADO INTEGER DEFAULT 0,
                $COL_SYNC_STATUS TEXT DEFAULT 'SYNCED'
            )
        """.trimIndent()
        db.execSQL(createTableSql)

        // Carga inicial de usuarios de prueba en la base de datos local
        insertInitialUsers(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        ensureEssentialUsers(db)
    }

    private fun insertInitialUsers(db: SQLiteDatabase) {
        val initialUsers = getEssentialUsersList()
        for (user in initialUsers) {
            val values = ContentValues().apply {
                put(COL_NOMBRE, user.nombres)
                put(COL_APELLIDO, user.apellidos)
                put(COL_EMAIL, user.email)
                put(COL_CLAVE, user.clave)
                put(COL_BLOQUEADO, 0)
                put(COL_SYNC_STATUS, "SYNCED")
            }
            db.insert(TABLE_USUARIOS, null, values)
        }
    }

    private fun getEssentialUsersList(): List<User> {
        return listOf(
            User(1, "Jorge Luis", "Cortés Gallardo", "jorge.cortes@correo.cl", "Password123!"),
            User(2, "Ana María", "Pérez Soto", "ana.perez@correo.cl", "SecurePass1#"),
            User(3, "Carlos Andrés", "González Muñoz", "carlos.gonzalez@correo.cl", "Admin2025*"),
            User(4, "Valentina Paz", "Silva Castro", "valentina.silva@correo.cl", "ClaveTest8$"),
            User(5, "Roberto Ignacio", "López Herrera", "roberto.lopez@correo.cl", "PassIot2025@"),
            User(6, "Emanuel Jesús", "Román Serón", "emanuelromanseron@gmail.com", "Ejrs2003.")
        )
    }

    private fun ensureEssentialUsers(db: SQLiteDatabase) {
        for (user in getEssentialUsersList()) {
            val cursor = db.rawQuery(
                "SELECT $COL_ID, $COL_SYNC_STATUS FROM $TABLE_USUARIOS WHERE LOWER($COL_EMAIL) = LOWER(?)",
                arrayOf(user.email.trim())
            )
            if (cursor.moveToFirst()) {
                val status = cursor.getString(1)
                if (status != "PENDING_DELETE" && status != "PENDING_UPDATE") {
                    val values = ContentValues().apply {
                        put(COL_NOMBRE, user.nombres)
                        put(COL_APELLIDO, user.apellidos)
                        put(COL_CLAVE, user.clave)
                        put(COL_BLOQUEADO, 0)
                    }
                    db.update(TABLE_USUARIOS, values, "LOWER($COL_EMAIL) = LOWER(?)", arrayOf(user.email.trim()))
                }
            } else {
                val values = ContentValues().apply {
                    put(COL_NOMBRE, user.nombres)
                    put(COL_APELLIDO, user.apellidos)
                    put(COL_EMAIL, user.email)
                    put(COL_CLAVE, user.clave)
                    put(COL_BLOQUEADO, 0)
                    put(COL_SYNC_STATUS, "SYNCED")
                }
                db.insert(TABLE_USUARIOS, null, values)
            }
            cursor.close()
        }
    }

    // ==========================================
    // OPERACIONES CRUD LOCALES
    // ==========================================

    fun insertarUsuario(user: User, pendingSync: Boolean = false): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOMBRE, user.nombres.trim())
            put(COL_APELLIDO, user.apellidos.trim())
            put(COL_EMAIL, user.email.trim())
            put(COL_CLAVE, user.clave)
            put(COL_BLOQUEADO, if (user.isBlocked) 1 else 0)
            put(COL_SYNC_STATUS, if (pendingSync) "PENDING_INSERT" else "SYNCED")
        }
        val id = db.insert(TABLE_USUARIOS, null, values)
        db.close()
        return id
    }

    fun obtenerUsuarios(): ArrayList<User> {
        val lista = ArrayList<User>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USUARIOS WHERE $COL_SYNC_STATUS != 'PENDING_DELETE'", null)

        if (cursor.moveToFirst()) {
            do {
                val user = User(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    nombres = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOMBRE)),
                    apellidos = cursor.getString(cursor.getColumnIndexOrThrow(COL_APELLIDO)),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                    clave = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLAVE)),
                    isBlocked = cursor.getInt(cursor.getColumnIndexOrThrow(COL_BLOQUEADO)) == 1,
                    syncStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_SYNC_STATUS))
                )
                lista.add(user)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    fun obtenerUsuarioPorEmail(email: String): User? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USUARIOS WHERE LOWER($COL_EMAIL) = LOWER(?) AND $COL_SYNC_STATUS != 'PENDING_DELETE'",
            arrayOf(email.trim())
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                nombres = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOMBRE)),
                apellidos = cursor.getString(cursor.getColumnIndexOrThrow(COL_APELLIDO)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                clave = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLAVE)),
                isBlocked = cursor.getInt(cursor.getColumnIndexOrThrow(COL_BLOQUEADO)) == 1,
                syncStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_SYNC_STATUS))
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun obtenerUsuarioPorId(id: Int): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USUARIOS WHERE $COL_ID = ?", arrayOf(id.toString()))
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                nombres = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOMBRE)),
                apellidos = cursor.getString(cursor.getColumnIndexOrThrow(COL_APELLIDO)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                clave = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLAVE)),
                isBlocked = cursor.getInt(cursor.getColumnIndexOrThrow(COL_BLOQUEADO)) == 1,
                syncStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_SYNC_STATUS))
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun sincronizarUsuariosRemotos(usuarios: List<User>) {
        val db = writableDatabase

        // 1. Upsert de usuarios remotos (sin sobreescribir cambios locales pendientes)
        for (u in usuarios) {
            val existing = db.rawQuery(
                "SELECT $COL_SYNC_STATUS, $COL_CLAVE FROM $TABLE_USUARIOS WHERE LOWER($COL_EMAIL) = LOWER(?)",
                arrayOf(u.email.trim())
            )
            var localStatus: String? = null
            var localClave: String? = null
            if (existing.moveToFirst()) {
                localStatus = existing.getString(0)
                localClave = existing.getString(1)
            }
            existing.close()

            // Si hay un cambio local pendiente, respetarlo
            if (localStatus == "PENDING_DELETE" || localStatus == "PENDING_INSERT" || localStatus == "PENDING_UPDATE") {
                continue
            }

            val values = ContentValues().apply {
                put(COL_NOMBRE, u.nombres)
                put(COL_APELLIDO, u.apellidos)
                put(COL_EMAIL, u.email)
                val finalClave = if (u.clave.isNotEmpty()) u.clave else (localClave ?: "Password123!")
                put(COL_CLAVE, finalClave)
                put(COL_BLOQUEADO, if (u.isBlocked) 1 else 0)
                put(COL_SYNC_STATUS, "SYNCED")
            }
            val updated = db.update(TABLE_USUARIOS, values, "LOWER($COL_EMAIL) = LOWER(?)", arrayOf(u.email.trim()))
            if (updated == 0) {
                db.insert(TABLE_USUARIOS, null, values)
            }
        }

        // 2. Eliminar de SQLite los registros SYNCED que ya NO existen en MySQL
        //    (fueron borrados desde otro dispositivo o directamente en el servidor)
        if (usuarios.isNotEmpty()) {
            val remoteEmails = usuarios.map { it.email.trim().lowercase() }.toSet()
            val cursor = db.rawQuery(
                "SELECT $COL_ID, $COL_EMAIL FROM $TABLE_USUARIOS WHERE $COL_SYNC_STATUS = 'SYNCED'",
                null
            )
            val toDelete = mutableListOf<Int>()
            if (cursor.moveToFirst()) {
                do {
                    val id    = cursor.getInt(0)
                    val email = cursor.getString(1).trim().lowercase()
                    if (email !in remoteEmails) {
                        toDelete.add(id)
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            for (id in toDelete) {
                db.delete(TABLE_USUARIOS, "$COL_ID = ?", arrayOf(id.toString()))
            }
        }

        db.close()
    }

    fun actualizarUsuario(id: Int, nombres: String, apellidos: String, email: String, isOffline: Boolean = false): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOMBRE, nombres.trim())
            put(COL_APELLIDO, apellidos.trim())
            put(COL_EMAIL, email.trim())
            if (isOffline) {
                put(COL_SYNC_STATUS, "PENDING_UPDATE")
            }
        }
        var affected = db.update(TABLE_USUARIOS, values, "$COL_ID = ?", arrayOf(id.toString()))
        if (affected == 0) {
            affected = db.update(TABLE_USUARIOS, values, "LOWER($COL_EMAIL) = LOWER(?)", arrayOf(email.trim()))
        }
        db.close()
        return affected
    }

    fun actualizarClave(email: String, nuevaClave: String, isOffline: Boolean = false): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CLAVE, nuevaClave)
            put(COL_BLOQUEADO, 0) // Al restablecer clave se reactiva la cuenta
            if (isOffline) {
                put(COL_SYNC_STATUS, "PENDING_UPDATE")
            }
        }
        val affected = db.update(TABLE_USUARIOS, values, "LOWER($COL_EMAIL) = LOWER(?)", arrayOf(email.trim()))
        db.close()
        return affected > 0
    }

    fun setBloqueoCuenta(email: String, bloqueado: Boolean): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_BLOQUEADO, if (bloqueado) 1 else 0)
        }
        val affected = db.update(TABLE_USUARIOS, values, "LOWER($COL_EMAIL) = LOWER(?)", arrayOf(email.trim()))
        db.close()
        return affected > 0
    }

    fun eliminarUsuario(id: Int, email: String = "", isOffline: Boolean = false): Boolean {
        val db = writableDatabase
        val affected = if (isOffline) {
            // Soft delete para que cuando haya internet se sincronice el DELETE en MySQL
            val values = ContentValues().apply {
                put(COL_SYNC_STATUS, "PENDING_DELETE")
            }
            var rows = db.update(TABLE_USUARIOS, values, "$COL_ID = ?", arrayOf(id.toString()))
            if (rows == 0 && email.isNotEmpty()) {
                rows = db.update(TABLE_USUARIOS, values, "LOWER($COL_EMAIL) = LOWER(?)", arrayOf(email.trim()))
            }
            rows
        } else {
            var rows = db.delete(TABLE_USUARIOS, "$COL_ID = ?", arrayOf(id.toString()))
            if (rows == 0 && email.isNotEmpty()) {
                rows = db.delete(TABLE_USUARIOS, "LOWER($COL_EMAIL) = LOWER(?)", arrayOf(email.trim()))
            }
            rows
        }
        db.close()
        return affected > 0
    }

    // ==========================================
    // MÉTODOS DE SINCRONIZACIÓN OFFLINE -> MYSQL
    // ==========================================

    fun obtenerCambiosPendientes(): List<User> {
        val pendientes = ArrayList<User>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USUARIOS WHERE $COL_SYNC_STATUS != 'SYNCED'", null)

        if (cursor.moveToFirst()) {
            do {
                val user = User(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    nombres = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOMBRE)),
                    apellidos = cursor.getString(cursor.getColumnIndexOrThrow(COL_APELLIDO)),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                    clave = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLAVE)),
                    isBlocked = cursor.getInt(cursor.getColumnIndexOrThrow(COL_BLOQUEADO)) == 1,
                    syncStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_SYNC_STATUS))
                )
                pendientes.add(user)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return pendientes
    }

    fun marcarSincronizado(id: Int, email: String = "") {
        val db = writableDatabase
        val cursor = if (email.isNotEmpty()) {
            db.rawQuery("SELECT $COL_SYNC_STATUS, $COL_ID FROM $TABLE_USUARIOS WHERE $COL_ID = ? OR LOWER($COL_EMAIL) = LOWER(?)", arrayOf(id.toString(), email.trim()))
        } else {
            db.rawQuery("SELECT $COL_SYNC_STATUS, $COL_ID FROM $TABLE_USUARIOS WHERE $COL_ID = ?", arrayOf(id.toString()))
        }
        if (cursor.moveToFirst()) {
            val status = cursor.getString(0)
            val realId = cursor.getInt(1)
            if (status == "PENDING_DELETE") {
                db.delete(TABLE_USUARIOS, "$COL_ID = ?", arrayOf(realId.toString()))
            } else {
                val values = ContentValues().apply {
                    put(COL_SYNC_STATUS, "SYNCED")
                }
                db.update(TABLE_USUARIOS, values, "$COL_ID = ?", arrayOf(realId.toString()))
            }
        }
        cursor.close()
        db.close()
    }

    fun upsertUsuarioLocal(id: Int, nombres: String, apellidos: String, email: String, clave: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOMBRE, nombres.trim())
            put(COL_APELLIDO, apellidos.trim())
            put(COL_EMAIL, email.trim())
            put(COL_CLAVE, clave)
            put(COL_BLOQUEADO, 0)
            put(COL_SYNC_STATUS, "SYNCED")
        }
        val affected = db.update(TABLE_USUARIOS, values, "LOWER($COL_EMAIL) = LOWER(?)", arrayOf(email.trim()))
        if (affected == 0) {
            db.insert(TABLE_USUARIOS, null, values)
        }
        db.close()
    }

    companion object {
        private const val DATABASE_NAME = "APPSQLITE"
        private const val DATABASE_VERSION = 3

        const val TABLE_USUARIOS = "USUARIOS"
        const val COL_ID = "ID"
        const val COL_NOMBRE = "NOMBRE"
        const val COL_APELLIDO = "APELLIDO"
        const val COL_EMAIL = "EMAIL"
        const val COL_CLAVE = "CLAVE"
        const val COL_BLOQUEADO = "BLOQUEADO"
        const val COL_SYNC_STATUS = "SYNC_STATUS"
    }
}
