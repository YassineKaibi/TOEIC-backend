package com.toeic.backend.quizzes

import com.toeic.backend.db.dbQuery
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.UUID

class QuizRepository {

    suspend fun createQuiz(
        teacherId: String,
        dto: CreateQuizDto
    ): QuizResponse {

        val quizId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        dbQuery {
            QuizzesTable.insert {
                it[id] = quizId
                it[title] = dto.title
                it[description] = dto.description
                it[timeLimitMinutes] = dto.timeLimitMinutes
                it[this.teacherId] = teacherId
                it[createdAt] = now
                it[updatedAt] = now
                it[archived] = false
            }
        }

        return getQuiz(quizId)!!
    }

    suspend fun updateQuiz(
        quizId: String,
        dto: UpdateQuizDto
    ): QuizResponse? {

        val rowsUpdated = dbQuery {
            QuizzesTable.update({ QuizzesTable.id eq quizId }) {
                dto.title?.let { title -> it[QuizzesTable.title] = title }
                dto.description?.let { desc -> it[description] = desc }
                dto.timeLimitMinutes?.let { time -> it[timeLimitMinutes] = time }
                it[updatedAt] = LocalDateTime.now()
            }
        }

        return if (rowsUpdated > 0) getQuiz(quizId) else null
    }

    suspend fun archiveQuiz(quizId: String): Boolean = dbQuery {
        val rowsUpdated = QuizzesTable.update({ QuizzesTable.id eq quizId }) {
            it[archived] = true
            it[updatedAt] = LocalDateTime.now()
        }
        rowsUpdated > 0
    }

    suspend fun getTeacherQuizzes(teacherId: String): List<QuizResponse> = dbQuery {
        QuizzesTable.selectAll()
            .where { (QuizzesTable.teacherId eq teacherId) and (QuizzesTable.archived eq false) }
            .map { it.toQuizResponse() }
    }

    suspend fun getQuiz(quizId: String): QuizResponse? {
        val quizRow = dbQuery {
            QuizzesTable.selectAll()
                .where { (QuizzesTable.id eq quizId) and (QuizzesTable.archived eq false) }
                .singleOrNull()
        } ?: return null

        val questions = getQuizQuestions(quizId)

        return quizRow.toQuizResponse(questions)
    }

    suspend fun createQuestion(
        quizId: String,
        dto: CreateQuestionDto
    ): QuestionResponse {

        val questionId = UUID.randomUUID().toString()

        dbQuery {
            QuestionsTable.insert {
                it[id] = questionId
                it[this.quizId] = quizId
                it[prompt] = dto.prompt
                it[order] = dto.order
                it[points] = dto.points
                it[options] = Json.encodeToString(dto.options)
                it[correctAnswer] = dto.correctAnswer
            }
        }

        return getQuestion(questionId)!!
    }

    suspend fun updateQuestion(
        questionId: String,
        dto: UpdateQuestionDto
    ): QuestionResponse? {

        val rowsUpdated = dbQuery {
            QuestionsTable.update({ QuestionsTable.id eq questionId }) {
                dto.prompt?.let { p -> it[prompt] = p }
                dto.order?.let { o -> it[order] = o }
                dto.points?.let { p -> it[points] = p }
                dto.options?.let { opt -> it[options] = Json.encodeToString(opt) }
                dto.correctAnswer?.let { c -> it[correctAnswer] = c }
            }
        }

        return if (rowsUpdated > 0) getQuestion(questionId) else null
    }

    suspend fun deleteQuestion(questionId: String): Boolean = dbQuery {
        val rowsDeleted = QuestionsTable.deleteWhere { QuestionsTable.id eq questionId }
        rowsDeleted > 0
    }
    
    suspend fun getQuestion(questionId: String): QuestionResponse? = dbQuery {
        QuestionsTable.selectAll()
            .where { QuestionsTable.id eq questionId }
            .map { it.toQuestionResponse() }
            .singleOrNull()
    }

    suspend fun getQuizQuestions(quizId: String): List<QuestionResponse> = dbQuery {
        QuestionsTable.selectAll()
            .where { QuestionsTable.quizId eq quizId }
            .orderBy(QuestionsTable.order to SortOrder.ASC)
            .map { it.toQuestionResponse() }
    }
    
    suspend fun getQuizOwner(quizId: String): String? = dbQuery {
        QuizzesTable.select(QuizzesTable.teacherId)
            .where { QuizzesTable.id eq quizId }
            .singleOrNull()?.get(QuizzesTable.teacherId)
    }

    private fun ResultRow.toQuizResponse(questions: List<QuestionResponse>? = null) = QuizResponse(
        id = this[QuizzesTable.id],
        title = this[QuizzesTable.title],
        description = this[QuizzesTable.description],
        timeLimitMinutes = this[QuizzesTable.timeLimitMinutes],
        teacherId = this[QuizzesTable.teacherId],
        createdAt = this[QuizzesTable.createdAt].toString(),
        questions = questions
    )

    private fun ResultRow.toQuestionResponse() = QuestionResponse(
        id = this[QuestionsTable.id],
        quizId = this[QuestionsTable.quizId],
        prompt = this[QuestionsTable.prompt],
        order = this[QuestionsTable.order],
        points = this[QuestionsTable.points],
        options = Json.decodeFromString(this[QuestionsTable.options]),
        correctAnswer = this[QuestionsTable.correctAnswer]
    )
}
