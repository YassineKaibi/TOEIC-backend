package com.toeic.backend.quizzes

import com.toeic.backend.classes.ClassesTable
import org.jetbrains.exposed.sql.Table

object QuizClassesTable : Table("quiz_classes") {
    val quizId  = varchar("quiz_id", 50).references(QuizzesTable.id)
    val classId = varchar("class_id", 50).references(ClassesTable.id)

    override val primaryKey = PrimaryKey(quizId, classId)
}
