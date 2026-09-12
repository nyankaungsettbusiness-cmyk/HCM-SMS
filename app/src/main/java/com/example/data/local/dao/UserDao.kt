package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.LoginHistoryEntity
import com.example.data.local.entity.RolePermissionEntity
import com.example.data.local.entity.SecurityPolicyEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserRole
import com.example.data.local.entity.UserStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT COUNT(*) FROM users WHERE isDeleted = 0")
    suspend fun getUserCount(): Int

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getTotalUserCount(): Int

    @Query("SELECT * FROM users WHERE isDeleted = 0 ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = :role AND isDeleted = 0")
    fun getUsersByRole(role: UserRole): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT * FROM users WHERE isDirty = 1")
    suspend fun getUsersForSync(): List<UserEntity>

    @Query("SELECT * FROM users WHERE uuid = :uuid LIMIT 1")
    suspend fun getUserByUuid(uuid: String): UserEntity?

    @Query("UPDATE users SET isDirty = 0, uuid = :uuid WHERE id = :id")
    suspend fun markUserSynced(id: Long, uuid: String)

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) AND isDeleted = 0 LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsernameIncludingDeleted(username: String): UserEntity?

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isDeleted = 1, status = 'INACTIVE', isDirty = 1, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun softDeleteUser(userId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Long)

    @Query("UPDATE users SET status = :status, failedLoginAttempts = CASE WHEN :status = 'ACTIVE' THEN 0 ELSE failedLoginAttempts END, isDirty = 1, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateUserStatus(userId: Long, status: UserStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET passwordHash = :passwordHash, salt = :salt, managedPassword = :managedPassword, mustChangePassword = :mustChangePassword, failedLoginAttempts = 0, isDirty = 1, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun resetPassword(userId: Long, passwordHash: String, salt: String, managedPassword: String, mustChangePassword: Boolean, updatedAt: Long = System.currentTimeMillis())

    // Role Permissions
    @Query("SELECT * FROM role_permissions")
    fun getAllRolePermissions(): Flow<List<RolePermissionEntity>>

    @Query("SELECT * FROM role_permissions WHERE role = :role")
    fun getPermissionsForRole(role: UserRole): Flow<List<RolePermissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePermission(permission: RolePermissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRolePermissions(permissions: List<RolePermissionEntity>)

    // Audit logs & Login History
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 150")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM login_history ORDER BY loginTime DESC LIMIT 200")
    fun getLoginHistory(): Flow<List<LoginHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoginHistory(history: LoginHistoryEntity)

    // Security Policy
    @Query("SELECT * FROM security_policy WHERE id = 1 LIMIT 1")
    fun getSecurityPolicyFlow(): Flow<SecurityPolicyEntity?>

    @Query("SELECT * FROM security_policy WHERE id = 1 LIMIT 1")
    suspend fun getSecurityPolicy(): SecurityPolicyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSecurityPolicy(policy: SecurityPolicyEntity)
}
