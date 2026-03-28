package com.toeic.backend.users

import com.toeic.backend.db.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class UserRepository {

    suspend fun insert(id: String, fullName: String, email: String, passwordHash: String, role: String) = dbQuery {
        UsersTable.insert {
            it[UsersTable.id] = id
            it[UsersTable.fullName] = fullName
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role] = role
        }
    }

    suspend fun findByEmail(email: String): UserRow? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.email eq email }
            .map { it.toUserRow() }
            .singleOrNull()
    }

    suspend fun findById(id: String): UserRow? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .map { it.toUserRow() }
            .singleOrNull()
    }

    private fun ResultRow.toUserRow() = UserRow(
        id = this[UsersTable.id],
        fullName = this[UsersTable.fullName],
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        role = this[UsersTable.role]
    )
}
