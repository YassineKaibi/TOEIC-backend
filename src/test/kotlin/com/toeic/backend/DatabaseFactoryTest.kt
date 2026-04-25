package com.toeic.backend

import com.toeic.backend.db.dbQuery
import com.toeic.backend.users.UsersTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseFactoryTest {

    @Test
    fun seed_inserts_exactly_three_users() = runBlocking {
        initTestDatabase(seedQuizData = false)
        val count = dbQuery { UsersTable.selectAll().count() }
        assertEquals(3, count) // teacher-01, student-01, student-02
    }
}
