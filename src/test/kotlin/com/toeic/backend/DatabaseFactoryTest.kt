package com.toeic.backend

import com.toeic.backend.db.dbQuery
import com.toeic.backend.users.UsersTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseFactoryTest {

    @Test
    fun seed_data_is_not_duplicated_on_second_call() = runBlocking {
        initTestDatabase(seedQuizData = false)
        val count = dbQuery { UsersTable.selectAll().count() }
        assertEquals(3, count) // teacher-01, student-01, student-02
    }
}
