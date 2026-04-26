package com.toeic.backend.classes

import com.toeic.backend.db.dbQuery
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.*

class ClassRepository {

    suspend fun create(name: String, description: String?, joinCode: String, teacherId: String): ClassResponse = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = Clock.System.now()

        ClassesTable.insert {
            it[ClassesTable.id] = id
            it[ClassesTable.name] = name
            it[ClassesTable.description] = description
            it[ClassesTable.joinCode] = joinCode
            it[ClassesTable.teacherId] = teacherId
            it[ClassesTable.createdAt] = now
        }

        ClassResponse(
            id = id,
            name = name,
            description = description,
            joinCode = joinCode,
            teacherId = teacherId,
            createdAt = now.toString()
        )
    }

    suspend fun findByJoinCode(code: String): ClassResponse? = dbQuery {
        ClassesTable.selectAll()
            .where { ClassesTable.joinCode eq code }
            .map {
                ClassResponse(
                    id = it[ClassesTable.id],
                    name = it[ClassesTable.name],
                    description = it[ClassesTable.description],
                    joinCode = it[ClassesTable.joinCode],
                    teacherId = it[ClassesTable.teacherId],
                    createdAt = it[ClassesTable.createdAt].toString()
                )
            }
            .singleOrNull()
    }

    suspend fun findById(id: String): ClassResponse? = dbQuery {
        ClassesTable.selectAll()
            .where { ClassesTable.id eq id }
            .map {
                ClassResponse(
                    id = it[ClassesTable.id],
                    name = it[ClassesTable.name],
                    description = it[ClassesTable.description],
                    joinCode = it[ClassesTable.joinCode],
                    teacherId = it[ClassesTable.teacherId],
                    createdAt = it[ClassesTable.createdAt].toString()
                )
            }
            .singleOrNull()
    }

    suspend fun findByTeacherId(
        teacherId: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Pair<List<ClassResponse>, Int> = dbQuery {
        val where = Op.build { ClassesTable.teacherId eq teacherId }
        val total = ClassesTable.selectAll().where(where).count().toInt()
        val items = ClassesTable.selectAll()
            .where(where)
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map {
                ClassResponse(
                    id = it[ClassesTable.id],
                    name = it[ClassesTable.name],
                    description = it[ClassesTable.description],
                    joinCode = it[ClassesTable.joinCode],
                    teacherId = it[ClassesTable.teacherId],
                    createdAt = it[ClassesTable.createdAt].toString()
                )
            }
        Pair(items, total)
    }
}
