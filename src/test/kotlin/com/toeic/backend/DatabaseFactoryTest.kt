package com.toeic.backend

import com.toeic.backend.db.DatabaseFactory
import com.toeic.backend.db.dbQuery
import com.toeic.backend.users.UsersTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseFactoryTest {

    @Test
    fun seed_inserts_exactly_three_users() = runBlocking {
        initTestDatabase(seedQuizData = false)
        val count = dbQuery { UsersTable.selectAll().count() }
        assertEquals(3, count)
        val hash = transaction(DatabaseFactory.database) {
            UsersTable.selectAll().first()[UsersTable.passwordHash]
        }
        assertTrue(hash.startsWith("\$2a\$") || hash.startsWith("\$2b\$"), "Expected a BCrypt hash but got: $hash")
    }
}
