package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.LoginHistoryEntity
import com.example.data.local.entity.RolePermissionEntity
import com.example.data.local.entity.SecurityPolicyEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.data.local.entity.UserStatus
import com.example.data.repository.AuthRepository
import com.example.data.sync.DeletionVerificationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _deletionVerificationEvent = MutableSharedFlow<DeletionVerificationResult>(extraBufferCapacity = 1)
    val deletionVerificationEvent: SharedFlow<DeletionVerificationResult> = _deletionVerificationEvent.asSharedFlow()

    val currentUser: StateFlow<UserEntity?> = repository.currentUser

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val rolePermissions: StateFlow<List<RolePermissionEntity>> = repository.allRolePermissions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val loginHistory: StateFlow<List<LoginHistoryEntity>> = repository.loginHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val securityPolicy: StateFlow<SecurityPolicyEntity?> = repository.securityPolicy.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SecurityPolicyEntity()
    )

    // Search and Filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSyncingUsers = MutableStateFlow(false)
    val isSyncingUsers: StateFlow<Boolean> = _isSyncingUsers.asStateFlow()

    private val _selectedRoleFilter = MutableStateFlow<UserRole?>(null)
    val selectedRoleFilter: StateFlow<UserRole?> = _selectedRoleFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<UserStatus?>(null)
    val selectedStatusFilter: StateFlow<UserStatus?> = _selectedStatusFilter.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    fun syncCloudUsers(onComplete: ((Result<Int>) -> Unit)? = null) {
        viewModelScope.launch {
            _isSyncingUsers.value = true
            val res = repository.syncUsersFromCloud()
            _isSyncingUsers.value = false
            onComplete?.invoke(res)
        }
    }

    fun cleanUpMockAccounts() {
        viewModelScope.launch {
            repository.cleanUpHardcodedDemoUsers()
        }
    }

    fun seedDefaultAdmin(onComplete: (Result<UserEntity>) -> Unit) {
        viewModelScope.launch {
            val res = repository.seedInitialDefaultAdmin()
            onComplete(res)
        }
    }

    fun removeDefaultAdmin(onComplete: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            val res = repository.removeDefaultAdminAccount()
            onComplete(res)
        }
    }

    val filteredUsers: StateFlow<List<UserEntity>> = combine(
        allUsers,
        searchQuery,
        selectedRoleFilter,
        selectedStatusFilter
    ) { users, query, role, status ->
        users.filter { user ->
            val matchesQuery = query.isBlank() ||
                    user.username.contains(query, ignoreCase = true) ||
                    user.fullName.contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true)
            val matchesRole = role == null || user.role == role
            val matchesStatus = status == null || user.status == status
            matchesQuery && matchesRole && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRoleFilter(role: UserRole?) {
        _selectedRoleFilter.value = role
    }

    fun setStatusFilter(status: UserStatus?) {
        _selectedStatusFilter.value = status
    }

    fun restoreSession(context: android.content.Context) {
        viewModelScope.launch {
            _isInitializing.value = true
            repository.restoreSession(context)
            _isInitializing.value = false
        }
    }

    fun loginWithCredentials(
        usernameInput: String,
        passwordInput: String,
        context: android.content.Context? = null,
        onResult: (Result<UserEntity>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.loginWithCredentials(usernameInput, passwordInput, context)
            onResult(res)
        }
    }

    fun loginAsRole(role: UserRole) {
        viewModelScope.launch {
            repository.loginAsRole(role)
        }
    }

    fun logout(context: android.content.Context? = null) {
        viewModelScope.launch {
            repository.logout(context)
        }
    }

    fun changeCurrentPassword(
        userId: Long,
        currentPass: String,
        newPass: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.changeCurrentPassword(userId, currentPass, newPass)
            onResult(res)
        }
    }

    fun updatePermission(permission: RolePermissionEntity) {
        viewModelScope.launch {
            repository.updateRolePermission(permission)
        }
    }

    fun hasPermission(moduleKey: String, actionKey: String): Boolean {
        val user = currentUser.value ?: return false
        if (user.role == UserRole.SUPER_ADMIN) return true
        val permKey = "${moduleKey}_${actionKey}"
        return rolePermissions.value.find { it.role == user.role && it.permissionKey == permKey }?.isAllowed ?: false
    }

    fun createUser(
        username: String,
        fullName: String,
        role: UserRole,
        email: String = "",
        phone: String = "",
        status: UserStatus = UserStatus.ACTIVE,
        linkedTeacherName: String = "",
        linkedStaffName: String = "",
        initialPassword: String = "123456",
        mustChangePassword: Boolean = true,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createUser(
                username = username,
                fullName = fullName,
                role = role,
                email = email,
                phone = phone,
                status = status,
                linkedTeacherName = linkedTeacherName,
                linkedStaffName = linkedStaffName,
                initialPassword = initialPassword,
                mustChangePassword = mustChangePassword
            )
            onResult(res)
        }
    }

    fun updateUser(user: UserEntity, onResult: ((Result<Unit>) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.updateUser(user)
            onResult?.invoke(res)
        }
    }

    fun setUserStatus(userId: Long, newStatus: UserStatus, onResult: ((Result<Unit>) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.setUserStatus(userId, newStatus)
            onResult?.invoke(res)
        }
    }

    fun resetPassword(userId: Long, newPass: String, forceChangeOnLogin: Boolean, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val res = repository.resetPassword(userId, newPass, forceChangeOnLogin)
            onResult(res)
        }
    }

    fun softDeleteUser(
        userId: Long,
        onResult: ((Result<DeletionVerificationResult>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val res = repository.softDeleteUser(userId)
            res.getOrNull()?.let { verification ->
                _deletionVerificationEvent.emit(verification)
            }
            onResult?.invoke(res)
        }
    }

    fun saveSecurityPolicy(policy: SecurityPolicyEntity, onResult: ((Result<Unit>) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.saveSecurityPolicy(policy)
            onResult?.invoke(res)
        }
    }

    fun addUser(username: String, fullName: String, role: UserRole, email: String) {
        viewModelScope.launch {
            repository.createUser(
                username = username,
                fullName = fullName,
                role = role,
                email = email
            )
        }
    }

    val syncStatus: StateFlow<com.example.data.sync.SyncStatus> = com.example.data.sync.SyncManager.syncState

    fun syncWithCloud(context: android.content.Context? = null, onResult: ((com.example.data.sync.SyncResult) -> Unit)? = null) {
        viewModelScope.launch {
            val res = com.example.data.sync.SyncManager.performSync(context, forceImmediate = true)
            onResult?.invoke(res)
        }
    }

    fun syncUsersOnly(context: android.content.Context? = null, onResult: ((Pair<Int, Int>) -> Unit)? = null) {
        viewModelScope.launch {
            _isSyncingUsers.value = true
            val res = com.example.data.sync.SyncManager.performSingleTableSync("users", context, forceImmediate = true)
            _isSyncingUsers.value = false
            onResult?.invoke(res)
        }
    }

    fun deleteUser(userId: Long, onResult: ((Result<DeletionVerificationResult>) -> Unit)? = null) {
        softDeleteUser(userId, onResult)
    }
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
