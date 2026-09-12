package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.LoginHistoryEntity
import com.example.data.local.entity.RolePermissionEntity
import com.example.data.local.entity.SecurityPolicyEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.data.local.entity.UserStatus
import com.example.data.remote.SupabaseClientManager
import com.example.data.sync.DeletionVerificationResult
import com.example.data.sync.SyncManager
import com.example.data.sync.model.UserSupabaseDto
import com.example.util.PasswordHasher
import com.example.util.UserSessionManager
import io.github.jan.supabase.postgrest.from
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class AuthRepository(private val userDao: UserDao) {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allRolePermissions: Flow<List<RolePermissionEntity>> = userDao.getAllRolePermissions()
    val auditLogs: Flow<List<AuditLogEntity>> = userDao.getRecentAuditLogs()
    val loginHistory: Flow<List<LoginHistoryEntity>> = userDao.getLoginHistory()
    val securityPolicy: Flow<SecurityPolicyEntity?> = userDao.getSecurityPolicyFlow()

    suspend fun syncUsersFromCloud(): Result<Int> {
        val client = SupabaseClientManager.getInstance()
            ?: return Result.failure(Exception("Supabase client is not configured."))

        return try {
            val remoteList = client.from("users").select().decodeList<UserSupabaseDto>()
            var count = 0
            for (remote in remoteList) {
                if (remote.isDeleted == true) {
                    val local = if (remote.uuid.isNotBlank()) userDao.getUserByUuid(remote.uuid)
                        else userDao.getUserByUsernameIncludingDeleted(remote.username)
                    if (local != null) {
                        userDao.deleteUser(local.id)
                    }
                    continue
                }
                val localByUuid = if (remote.uuid.isNotBlank()) userDao.getUserByUuid(remote.uuid) else null
                val localByName = if (localByUuid == null && remote.username.isNotBlank()) userDao.getUserByUsernameIncludingDeleted(remote.username) else null
                val existingLocal = localByUuid ?: localByName

                val entity = remote.toEntity(existingLocalId = existingLocal?.id ?: 0)
                userDao.insertUser(entity)
                count++
            }
            Log.i("AuthRepository", "Successfully synced $count user accounts from Supabase cloud.")
            Result.success(count)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to sync user accounts from cloud: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun cleanUpHardcodedDemoUsers() {
        val allUsers = userDao.getAllUsersList()
        val mockUsernames = setOf("superadmin", "admin", "teacher", "staff")
        val mockEmails = setOf("super@hcm.edu.mm", "admin@hcm.edu.mm", "teacher@hcm.edu.mm", "staff@hcm.edu.mm")

        for (u in allUsers) {
            val isMock = (u.username.lowercase() in mockUsernames && u.email in mockEmails && u.passwordHash == "ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f")
            if (isMock) {
                userDao.deleteUser(u.id)
            }
        }
    }

    suspend fun removeDefaultAdminAccount(): Result<Boolean> {
        return try {
            val allUsers = userDao.getAllUsersList().filter { !it.isDeleted }
            val otherAdmins = allUsers.filter { 
                it.username.lowercase() != "admin" && (it.role == UserRole.ADMIN || it.role == UserRole.SUPER_ADMIN)
            }
            if (otherAdmins.isEmpty()) {
                return Result.failure(IllegalStateException("Cannot remove default admin: No other administrator account exists."))
            }

            val defaultAdmin = userDao.getUserByUsernameIncludingDeleted("admin")
            if (defaultAdmin != null) {
                userDao.deleteUser(defaultAdmin.id)
                // Propagate deletion to Supabase
                try {
                    SyncManager.verifyAndPropagateDeletion(
                        tableName = "users",
                        uuid = defaultAdmin.uuid,
                        codeOrKey = defaultAdmin.username,
                        fallbackRemoteId = defaultAdmin.id,
                        preferHardDelete = true
                    )
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Failed to delete default admin on cloud: ${e.message}")
                }
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error removing default admin: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun seedInitialDefaultAdmin(): Result<UserEntity> {
        return try {
            val allActive = userDao.getAllUsersList().filter { !it.isDeleted }
            val hasCustomAdmin = allActive.any { it.username.lowercase() != "admin" && (it.role == UserRole.ADMIN || it.role == UserRole.SUPER_ADMIN) }
            if (hasCustomAdmin) {
                return Result.failure(IllegalStateException("Custom admin account already exists. Default admin is disabled."))
            }
            val existing = userDao.getUserByUsernameIncludingDeleted("admin")
            val defaultAdmin = UserEntity(
                id = existing?.id ?: 0,
                username = "admin",
                fullName = "Super Administrator",
                role = UserRole.SUPER_ADMIN,
                email = "admin@hcm.edu.mm",
                phone = "+95 9 790001122",
                passwordHash = PasswordHasher.hashPassword("Password123!"),
                salt = PasswordHasher.DEFAULT_SALT,
                isActive = true,
                status = UserStatus.ACTIVE,
                mustChangePassword = false,
                failedLoginAttempts = 0,
                isDeleted = false,
                uuid = existing?.uuid?.ifBlank { java.util.UUID.randomUUID().toString() } ?: java.util.UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isDirty = true,
                managedPassword = "Password123!"
            )
            val newId = userDao.insertUser(defaultAdmin)
            val savedEntity = defaultAdmin.copy(id = if (existing != null && existing.id > 0) existing.id else newId)

            val client = SupabaseClientManager.getInstance()
            if (client != null) {
                try {
                    val dto = UserSupabaseDto.fromEntity(savedEntity)
                    client.from("users").upsert(dto, onConflict = "username")
                    userDao.markUserSynced(savedEntity.id, savedEntity.uuid)
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Could not immediately push auto-seeded admin to cloud: ${e.message}")
                }
            }
            Result.success(savedEntity)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to seed default admin: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun restoreSession(context: Context) {
        // Enforce explicit authentication: always require entering username & password on launch
        _currentUser.value = null
    }

    suspend fun loginWithCredentials(
        usernameInput: String,
        passwordInput: String,
        context: Context? = null
    ): Result<UserEntity> {
        val cleanUsername = usernameInput.trim()
        var user = userDao.getUserByUsername(cleanUsername)
            ?: userDao.getAllUsersList().firstOrNull { 
                !it.isDeleted && (
                    it.username.equals(cleanUsername, ignoreCase = true) ||
                    (it.email.isNotBlank() && it.email.equals(cleanUsername, ignoreCase = true))
                )
            }

        // If not found locally, attempt to pull from Supabase Cloud
        if (user == null) {
            val client = SupabaseClientManager.getInstance()
            if (client != null) {
                try {
                    val remotes = client.from("users").select().decodeList<UserSupabaseDto>()
                    val matchingRemote = remotes.firstOrNull { remote ->
                        !(remote.isDeleted ?: false) && (
                            remote.username.trim().equals(cleanUsername, ignoreCase = true) ||
                            (remote.email?.isNotBlank() == true && remote.email.trim().equals(cleanUsername, ignoreCase = true))
                        )
                    }
                    if (matchingRemote != null) {
                        val entity = matchingRemote.toEntity()
                        val existingLocal = userDao.getUserByUsernameIncludingDeleted(entity.username)
                        val insertedId = if (existingLocal != null) {
                            userDao.updateUser(entity.copy(id = existingLocal.id))
                            existingLocal.id
                        } else {
                            userDao.insertUser(entity)
                        }
                        user = entity.copy(id = insertedId)
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Cloud lookup on login failed: ${e.message}")
                }
            }
        }

        // If still null and user is attempting to login as 'admin', auto-seed initial default admin ONLY IF absolutely no other admin accounts exist
        if (user == null && cleanUsername.equals("admin", ignoreCase = true)) {
            val allActiveUsers = userDao.getAllUsersList().filter { !it.isDeleted }
            val hasAnyAdmin = allActiveUsers.any { it.role == UserRole.ADMIN || it.role == UserRole.SUPER_ADMIN }
            if (!hasAnyAdmin && allActiveUsers.isEmpty()) {
                val defaultAdmin = UserEntity(
                    username = "admin",
                    fullName = "Super Administrator",
                    role = UserRole.SUPER_ADMIN,
                    email = "admin@hcm.edu.mm",
                    phone = "+95 9 790001122",
                    passwordHash = PasswordHasher.hashPassword("Password123!"),
                    salt = PasswordHasher.DEFAULT_SALT,
                    isActive = true,
                    status = UserStatus.ACTIVE,
                    mustChangePassword = false,
                    failedLoginAttempts = 0,
                    isDeleted = false,
                    uuid = java.util.UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isDirty = false,
                    managedPassword = "Password123!"
                )
                val newId = userDao.insertUser(defaultAdmin)
                user = defaultAdmin.copy(id = newId)

                // Also push to Supabase Cloud so other devices immediately receive this admin account
                val client = SupabaseClientManager.getInstance()
                if (client != null) {
                    try {
                        val dto = UserSupabaseDto.fromEntity(user)
                        client.from("users").upsert(dto, onConflict = "username")
                        userDao.markUserSynced(user.id, user.uuid)
                    } catch (e: Exception) {
                        Log.w("AuthRepository", "Failed to sync first-time admin to cloud immediately: ${e.message}")
                    }
                }
            }
        }

        if (user == null) {
            return Result.failure(Exception("Account '@$cleanUsername' not found. Please verify your username or sync accounts from cloud."))
        }

        if (user.isDeleted) {
            return Result.failure(Exception("Account '@$cleanUsername' has been deleted."))
        }

        if (user.status == UserStatus.LOCKED || user.status == UserStatus.SUSPENDED || user.status == UserStatus.INACTIVE) {
            userDao.insertLoginHistory(
                LoginHistoryEntity(
                    userId = user.id,
                    username = user.username,
                    displayName = user.fullName,
                    status = "FAILED_LOCKED",
                    failureReason = "Account status is ${user.status.displayName}"
                )
            )
            return Result.failure(Exception("Account is ${user.status.displayName}. Please contact system administrator."))
        }

        val policy = userDao.getSecurityPolicy() ?: SecurityPolicyEntity()
        val isValidPassword = PasswordHasher.verifyPassword(passwordInput, user.salt, user.passwordHash)

        if (!isValidPassword) {
            val newFailedAttempts = user.failedLoginAttempts + 1
            val shouldLock = newFailedAttempts >= policy.maxFailedAttempts
            val updatedStatus = if (shouldLock) UserStatus.LOCKED else user.status

            val updatedUser = user.copy(
                failedLoginAttempts = newFailedAttempts,
                status = updatedStatus,
                isDirty = true,
                updatedAt = System.currentTimeMillis()
            )
            userDao.updateUser(updatedUser)

            userDao.insertLoginHistory(
                LoginHistoryEntity(
                    userId = user.id,
                    username = user.username,
                    displayName = user.fullName,
                    status = if (shouldLock) "FAILED_LOCKED" else "FAILED_INVALID_PASSWORD",
                    failureReason = if (shouldLock) "Locked after $newFailedAttempts failed attempts" else "Incorrect password attempt ($newFailedAttempts/${policy.maxFailedAttempts})"
                )
            )

            val errMsg = if (shouldLock) {
                "Account locked due to $newFailedAttempts consecutive failed login attempts."
            } else {
                "Invalid password. ${policy.maxFailedAttempts - newFailedAttempts} attempts remaining."
            }
            return Result.failure(Exception(errMsg))
        }

        // Login Success
        val effectiveSalt = if (user.salt.isBlank()) "HCM_SALT_2026" else user.salt
        val updatedHash = PasswordHasher.hashPassword(passwordInput, effectiveSalt)
        val loggedInUser = user.copy(
            passwordHash = updatedHash,
            salt = effectiveSalt,
            failedLoginAttempts = 0,
            lastLoginTimestamp = System.currentTimeMillis(),
            isDirty = user.isDirty,
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(loggedInUser)

        userDao.insertLoginHistory(
            LoginHistoryEntity(
                userId = loggedInUser.id,
                username = loggedInUser.username,
                displayName = loggedInUser.fullName,
                loginTime = System.currentTimeMillis(),
                device = "Android Device / Tablet",
                ipAddress = "192.168.1.10",
                status = "SUCCESS"
            )
        )

        logAction(loggedInUser.fullName, loggedInUser.role.name, "LOGIN", "Successfully logged into HCM-SMS")

        context?.let { UserSessionManager.saveSession(it, loggedInUser.username) }
        _currentUser.value = loggedInUser
        return Result.success(loggedInUser)
    }

    suspend fun loginAsRole(role: UserRole): UserEntity? {
        val userList = userDao.getAllUsersList().filter { it.role == role && it.status == UserStatus.ACTIVE && !it.isDeleted }
        val matchingUser = userList.firstOrNull() ?: _currentUser.value
        if (matchingUser != null) {
            _currentUser.value = matchingUser
        }
        return matchingUser
    }

    suspend fun logout(context: Context? = null) {
        _currentUser.value?.let { user ->
            userDao.insertLoginHistory(
                LoginHistoryEntity(
                    userId = user.id,
                    username = user.username,
                    displayName = user.fullName,
                    loginTime = System.currentTimeMillis(),
                    logoutTime = System.currentTimeMillis(),
                    device = "Android Device / Tablet",
                    ipAddress = "192.168.1.10",
                    status = "LOGOUT"
                )
            )
            logAction(user.fullName, user.role.name, "LOGOUT", "User logged out")
        }
        context?.let { UserSessionManager.clearSession(it) }
        _currentUser.value = null
    }

    suspend fun changeCurrentPassword(
        userId: Long,
        currentPasswordInput: String,
        newPasswordInput: String
    ): Result<Unit> {
        val user = userDao.getUserById(userId)
            ?: return Result.failure(Exception("User not found"))

        if (!PasswordHasher.verifyPassword(currentPasswordInput, user.salt, user.passwordHash)) {
            return Result.failure(Exception("Current password is incorrect."))
        }

        val newSalt = PasswordHasher.DEFAULT_SALT
        val newHash = PasswordHasher.hashPassword(newPasswordInput, newSalt)

        val updatedUser = user.copy(
            passwordHash = newHash,
            salt = newSalt,
            mustChangePassword = false,
            isDirty = true,
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updatedUser)

        if (_currentUser.value?.id == userId) {
            _currentUser.value = updatedUser
        }

        logAction(updatedUser.fullName, updatedUser.role.name, "CHANGE_PASSWORD", "User changed their password")
        triggerBackgroundSync()
        return Result.success(Unit)
    }

    suspend fun createUser(
        username: String,
        fullName: String,
        role: UserRole,
        email: String = "",
        phone: String = "",
        status: UserStatus = UserStatus.ACTIVE,
        linkedTeacherName: String = "",
        linkedStaffName: String = "",
        initialPassword: String = "123456",
        mustChangePassword: Boolean = false
    ): Result<Long> {
        val current = _currentUser.value
        if (current != null && current.role != UserRole.SUPER_ADMIN && current.role != UserRole.ADMIN) {
            return Result.failure(SecurityException("Permission denied: Only Administrators can create user accounts."))
        }

        val cleanUsername = username.trim().lowercase()
        if (cleanUsername.isBlank()) {
            return Result.failure(IllegalArgumentException("Username cannot be blank."))
        }

        val existing = userDao.getUserByUsername(cleanUsername)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Username '$cleanUsername' is already in use!"))
        }

        val salt = PasswordHasher.DEFAULT_SALT
        val passwordHash = PasswordHasher.hashPassword(initialPassword, salt)
        val generatedUuid = UUID.randomUUID().toString()

        val newUser = UserEntity(
            username = cleanUsername,
            fullName = fullName.trim(),
            role = role,
            email = email.trim(),
            phone = phone.trim(),
            linkedTeacherName = linkedTeacherName.trim(),
            linkedStaffName = linkedStaffName.trim(),
            passwordHash = passwordHash,
            salt = salt,
            status = status,
            isActive = (status == UserStatus.ACTIVE),
            createdAt = System.currentTimeMillis(),
            mustChangePassword = mustChangePassword,
            uuid = generatedUuid,
            isDirty = true,
            updatedAt = System.currentTimeMillis(),
            managedPassword = initialPassword
        )

        val id = userDao.insertUser(newUser)
        _currentUser.value?.let { performer ->
            logAction(performer.fullName, performer.role.name, "CREATE_USER", "Created account '${cleanUsername}' for ${fullName} (${role.displayName})")
        }
        triggerBackgroundSync()
        return Result.success(id)
    }

    suspend fun updateUser(user: UserEntity): Result<Unit> {
        val current = _currentUser.value
        if (current != null && current.role != UserRole.SUPER_ADMIN && current.role != UserRole.ADMIN && current.id != user.id) {
            return Result.failure(SecurityException("Permission denied: You cannot edit other user accounts."))
        }

        val existingUser = userDao.getUserById(user.id)
            ?: return Result.failure(IllegalArgumentException("User account not found."))

        // Super Admin account can only be edited by Super Admin
        if (existingUser.role == UserRole.SUPER_ADMIN && current?.role != UserRole.SUPER_ADMIN && current?.id != user.id) {
            return Result.failure(SecurityException("Permission denied: Only a Super Admin can modify a Super Admin account."))
        }

        // Validate username uniqueness if changed
        val cleanUsername = user.username.trim().lowercase()
        if (cleanUsername.isBlank()) {
            return Result.failure(IllegalArgumentException("Username cannot be blank."))
        }
        if (!cleanUsername.equals(existingUser.username, ignoreCase = true)) {
            val usernameTaken = userDao.getUserByUsername(cleanUsername)
            if (usernameTaken != null && usernameTaken.id != user.id) {
                return Result.failure(IllegalArgumentException("Username '$cleanUsername' is already taken by another account."))
            }
        }

        val dirtyUser = user.copy(
            username = cleanUsername,
            isActive = (user.status == UserStatus.ACTIVE),
            isDirty = true,
            updatedAt = System.currentTimeMillis()
        )
        userDao.updateUser(dirtyUser)
        _currentUser.value?.let { currentPerformer ->
            logAction(currentPerformer.fullName, currentPerformer.role.name, "UPDATE_USER", "Updated account details for ${dirtyUser.username}")
        }
        triggerBackgroundSync()
        return Result.success(Unit)
    }

    suspend fun setUserStatus(userId: Long, newStatus: UserStatus): Result<Unit> {
        val current = _currentUser.value
        if (current != null && current.role != UserRole.SUPER_ADMIN && current.role != UserRole.ADMIN) {
            return Result.failure(SecurityException("Permission denied: You cannot change account status."))
        }

        val user = userDao.getUserById(userId)
            ?: return Result.failure(IllegalArgumentException("User not found."))

        if (user.role == UserRole.SUPER_ADMIN && current?.role != UserRole.SUPER_ADMIN) {
            return Result.failure(SecurityException("Permission denied: Only a Super Admin can change a Super Admin account's status."))
        }

        userDao.updateUserStatus(userId, newStatus)
        _currentUser.value?.let { performer ->
            logAction(performer.fullName, performer.role.name, "STATUS_CHANGE", "Changed status of '${user.username}' to ${newStatus.displayName}")
        }
        triggerBackgroundSync()
        return Result.success(Unit)
    }

    suspend fun resetPassword(userId: Long, newPass: String, forceChangeOnLogin: Boolean): Result<String> {
        val current = _currentUser.value
        if (current != null && current.role != UserRole.SUPER_ADMIN && current.role != UserRole.ADMIN && current.id != userId) {
            return Result.failure(SecurityException("Permission denied: Only Administrators can reset passwords for other accounts."))
        }

        val targetUser = userDao.getUserById(userId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        if (targetUser.role == UserRole.SUPER_ADMIN && current?.role != UserRole.SUPER_ADMIN && current?.id != userId) {
            return Result.failure(SecurityException("Permission denied: Only a Super Admin can reset the password for a Super Admin account."))
        }

        val salt = PasswordHasher.DEFAULT_SALT
        val hash = PasswordHasher.hashPassword(newPass, salt)
        userDao.resetPassword(userId, hash, salt, managedPassword = newPass, mustChangePassword = forceChangeOnLogin)

        _currentUser.value?.let { performer ->
            logAction(performer.fullName, performer.role.name, "RESET_PASSWORD", "Reset password for '${targetUser.username}'")
        }
        triggerBackgroundSync()
        return Result.success(newPass)
    }

    private fun triggerBackgroundSync() {
        com.example.data.sync.SyncManager.triggerTableSyncAsync("users", forceImmediate = true)
        com.example.data.sync.SyncManager.triggerSyncAsync(forceImmediate = false)
    }

    suspend fun softDeleteUser(
        userId: Long,
        onVerificationResult: ((DeletionVerificationResult) -> Unit)? = null
    ): Result<DeletionVerificationResult> {
        val current = _currentUser.value
        if (current != null && current.role != UserRole.SUPER_ADMIN && current.role != UserRole.ADMIN) {
            return Result.failure(SecurityException("Permission denied: Only Administrators can delete user accounts."))
        }

        val targetUser = userDao.getUserById(userId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        userDao.softDeleteUser(userId)
        
        // If the user deleted the account they were currently logged into, log them out cleanly
        if (current?.id == userId) {
            _currentUser.value = null
        }

        _currentUser.value?.let { performer ->
            logAction(performer.fullName, performer.role.name, "DELETE_USER", "Deleted user account '${targetUser.username}'")
        }
        triggerBackgroundSync()

        val verification = SyncManager.verifyAndPropagateDeletion(
            tableName = "users",
            uuid = targetUser.uuid,
            codeOrKey = targetUser.username,
            fallbackRemoteId = targetUser.id,
            preferHardDelete = true
        )
        onVerificationResult?.invoke(verification)
        return Result.success(verification)
    }

    suspend fun updateRolePermission(permission: RolePermissionEntity): Result<Unit> {
        val current = _currentUser.value
        if (current != null && current.role != UserRole.SUPER_ADMIN && current.role != UserRole.ADMIN) {
            return Result.failure(SecurityException("Permission denied: Only Administrators can modify role permissions."))
        }

        userDao.insertOrUpdatePermission(permission)
        _currentUser.value?.let { user ->
            logAction(user.fullName, user.role.name, "PERMISSION_UPDATE", "Updated permission ${permission.permissionKey} for ${permission.role.displayName}")
        }
        return Result.success(Unit)
    }

    suspend fun saveSecurityPolicy(policy: SecurityPolicyEntity): Result<Unit> {
        val current = _currentUser.value
        if (current != null && current.role != UserRole.SUPER_ADMIN && current.role != UserRole.ADMIN) {
            return Result.failure(SecurityException("Permission denied: Only Administrators can modify security policies."))
        }

        userDao.saveSecurityPolicy(policy)
        _currentUser.value?.let { user ->
            logAction(user.fullName, user.role.name, "POLICY_UPDATE", "Updated security policy settings")
        }
        return Result.success(Unit)
    }

    fun hasPermissionFlow(moduleKey: String, actionKey: String): Flow<Boolean> {
        val permKey = "${moduleKey}_${actionKey}"
        return allRolePermissions.map { permissions ->
            val user = _currentUser.value ?: return@map false
            if (user.role == UserRole.SUPER_ADMIN) return@map true
            permissions.find { it.role == user.role && it.permissionKey == permKey }?.isAllowed ?: false
        }
    }

    suspend fun logAction(userName: String, roleName: String, action: String, details: String) {
        userDao.insertAuditLog(
            AuditLogEntity(
                userName = userName,
                roleName = roleName,
                action = action,
                details = details
            )
        )
    }

    suspend fun addUser(user: UserEntity): Long {
        val id = userDao.insertUser(user)
        _currentUser.value?.let { current ->
            logAction(current.fullName, current.role.name, "CREATE_USER", "Added new user: ${user.fullName} (${user.role.displayName})")
        }
        return id
    }

    suspend fun deleteUser(userId: Long) {
        userDao.softDeleteUser(userId)
    }
}
