package com.example.test.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.data.RemoteApiManager
import com.example.test.data.UserRepository
import com.example.test.databinding.ActivityUserListBinding
import com.example.test.model.User
import com.example.test.util.NetworkSyncManager
import com.example.test.util.ValidationUtils

class UserListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserListBinding
    private lateinit var adapter: UserAdapter
    private var allUsers: List<User> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(emptyList()) { selectedUser ->
            val intent = Intent(this, UserEditActivity::class.java)
            intent.putExtra("user_id", selectedUser.id)
            startActivity(intent)
        }
        binding.rvUsers.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter
    }

    private fun refreshList() {
        if (NetworkSyncManager.isOnline) {
            // ─── ONLINE: cargar desde MySQL ─────────────────────
            binding.tvEmptyState.visibility = View.GONE
            Toast.makeText(this, "Cargando usuarios desde MySQL (AWS)…", Toast.LENGTH_SHORT).show()

            RemoteApiManager.listUsers(this,
                onSuccess = { remoteList ->
                    allUsers = remoteList
                    filterUsers(binding.etSearchUser.text.toString())
                    binding.tvEmptyState.visibility =
                        if (remoteList.isEmpty()) View.VISIBLE else View.GONE
                    // Mostrar badge de fuente de datos si existe
                    showDataSourceBadge(fromRemote = true)
                },
                onError = { errorMsg ->
                    Toast.makeText(this, "Error MySQL: $errorMsg. Usando SQLite local.", Toast.LENGTH_LONG).show()
                    loadFromSQLite()
                }
            )
        } else {
            // ─── OFFLINE: cargar desde SQLite ────────────────────
            loadFromSQLite()
        }
    }

    private fun loadFromSQLite() {
        allUsers = UserRepository.getAllUsers()
        filterUsers(binding.etSearchUser.text.toString())
        binding.tvEmptyState.visibility =
            if (allUsers.isEmpty()) View.VISIBLE else View.GONE
        showDataSourceBadge(fromRemote = false)
    }

    /** Muestra opcionalmente un badge/toast indicando si los datos vienen de MySQL o SQLite */
    private fun showDataSourceBadge(fromRemote: Boolean) {
        val src = if (fromRemote) "MySQL (AWS)" else "SQLite (local)"
        val pending = UserRepository.getPendingSyncUsers().size
        val pendingMsg = if (pending > 0) " • $pending cambios pendientes de sincronizar" else ""
        Toast.makeText(this, "Fuente: $src$pendingMsg", Toast.LENGTH_SHORT).show()
    }

    private fun setupSearch() {
        binding.etSearchUser.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterUsers(query: String) {
        val cleanQuery = ValidationUtils.removeAccents(query.trim()).lowercase()
        val filtered = if (cleanQuery.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { user ->
                val fullName    = "${user.nombres} ${user.apellidos}"
                val cleanFullName = ValidationUtils.removeAccents(fullName).lowercase()
                cleanFullName.contains(cleanQuery)
            }
        }
        adapter.updateData(filtered)
        binding.tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}
