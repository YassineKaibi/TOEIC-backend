package com.toeic.backend.enrollments

import com.toeic.backend.classes.ClassStudentItem
import com.toeic.backend.classes.ClassesTable
import com.toeic.backend.classes.StudentClassItem
import com.toeic.backend.classes.TeacherInfo
import com.toeic.backend.db.dbQuery
import com.toeic.backend.users.UsersTable
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class EnrollmentRepository {

    suspend fun insert(classId: String, studentId: String) = dbQuery {
        EnrollmentsTable.insert {
            it[EnrollmentsTable.id] = UUID.randomUUID().toString()
            it[EnrollmentsTable.classId] = classId
            it[EnrollmentsTable.studentId] = studentId
            it[EnrollmentsTable.joinedAt] = Clock.System.now()
        }
    }

    suspend fun exists(classId: String, studentId: String): Boolean = dbQuery {
        EnrollmentsTable.selectAll()
            .where { (EnrollmentsTable.classId eq classId) and (EnrollmentsTable.studentId eq studentId) }
            .count() > 0
    }

    suspend fun delete(classId: String, studentId: String) = dbQuery {
        EnrollmentsTable.deleteWhere {
            (EnrollmentsTable.classId eq classId) and (EnrollmentsTable.studentId eq studentId)
        }
    }

    suspend fun countByClassId(classId: String): Int = dbQuery {
        EnrollmentsTable.selectAll()
            .where { EnrollmentsTable.classId eq classId }
            .count().toInt()
    }

    suspend fun findStudentsByClassId(
        classId: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Pair<List<ClassStudentItem>, Int> = dbQuery {
        val where = Op.build { EnrollmentsTable.classId eq classId }
        val total = (EnrollmentsTable innerJoin UsersTable)
            .selectAll().where(where).count().toInt()
        val items = (EnrollmentsTable innerJoin UsersTable)
            .selectAll()
            .where(where)
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map {
                ClassStudentItem(
                    id = it[UsersTable.id],
                    fullName = it[UsersTable.fullName],
                    email = it[UsersTable.email],
                    joinedAt = it[EnrollmentsTable.joinedAt].toString()
                )
            }
        Pair(items, total)
    }

    suspend fun findClassesByStudentId(
        studentId: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Pair<List<StudentClassItem>, Int> = dbQuery {
        val where = Op.build { EnrollmentsTable.studentId eq studentId }
        val total = EnrollmentsTable.selectAll().where(where).count().toInt()
        val items = EnrollmentsTable
            .innerJoin(ClassesTable)
            .join(UsersTable, JoinType.INNER, ClassesTable.teacherId, UsersTable.id)
            .selectAll()
            .where(where)
            .limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map {
                StudentClassItem(
                    id = it[ClassesTable.id],
                    name = it[ClassesTable.name],
                    teacher = TeacherInfo(fullName = it[UsersTable.fullName]),
                    joinedAt = it[EnrollmentsTable.joinedAt].toString()
                )
            }
        Pair(items, total)
    }
}
