package com.toeic.backend.quizzes

import com.toeic.backend.common.BadRequestException
import com.toeic.backend.common.ForbiddenException
import com.toeic.backend.common.NotFoundException

class QuizService(private val quizRepository: QuizRepository) {

    suspend fun createQuiz(teacherId: String, dto: CreateQuizDto): QuizResponse {
        validateQuizDto(dto.title, dto.timeLimitMinutes)
        return quizRepository.createQuiz(teacherId, dto)
    }

    suspend fun getTeacherQuizzes(
        teacherId: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Pair<List<QuizResponse>, Int> {
        return quizRepository.getTeacherQuizzes(teacherId, page, pageSize)
    }

    suspend fun getQuiz(quizId: String, teacherId: String): QuizResponse {
        val quiz = quizRepository.getQuiz(quizId) ?: throw NotFoundException("Quiz not found", "QUIZ_NOT_FOUND")
        if (quiz.teacherId != teacherId) {
            throw ForbiddenException("You don't own this quiz", "NOT_QUIZ_OWNER")
        }
        return quiz
    }

    suspend fun updateQuiz(quizId: String, teacherId: String, dto: UpdateQuizDto): QuizResponse {
        verifyOwnership(quizId, teacherId)
        dto.title?.let { validateQuizDto(it, dto.timeLimitMinutes) }
            ?: dto.timeLimitMinutes?.let { validateQuizDto(null, it) }
        return quizRepository.updateQuiz(quizId, dto) ?: throw NotFoundException("Quiz not found", "QUIZ_NOT_FOUND")
    }

    suspend fun archiveQuiz(quizId: String, teacherId: String) {
        verifyOwnership(quizId, teacherId)
        val success = quizRepository.archiveQuiz(quizId)
        if (!success) throw NotFoundException("Quiz not found", "QUIZ_NOT_FOUND")
    }

    suspend fun createQuestion(quizId: String, teacherId: String, dto: CreateQuestionDto): QuestionResponse {
        verifyOwnership(quizId, teacherId)
        validateCreateQuestionDto(dto)
        return quizRepository.createQuestion(quizId, dto)
    }

    suspend fun updateQuestion(quizId: String, questionId: String, teacherId: String, dto: UpdateQuestionDto): QuestionResponse {
        verifyOwnership(quizId, teacherId)
        verifyQuestionOwnership(quizId, questionId)
        validateUpdateQuestionDto(dto)
        return quizRepository.updateQuestion(questionId, dto) ?: throw NotFoundException("Question not found", "QUESTION_NOT_FOUND")
    }

    suspend fun deleteQuestion(quizId: String, questionId: String, teacherId: String) {
        verifyOwnership(quizId, teacherId)
        verifyQuestionOwnership(quizId, questionId)
        val success = quizRepository.deleteQuestion(questionId)
        if (!success) throw NotFoundException("Question not found", "QUESTION_NOT_FOUND")
    }

    private suspend fun verifyOwnership(quizId: String, teacherId: String) {
        val ownerId = quizRepository.getQuizOwner(quizId) ?: throw NotFoundException("Quiz not found", "QUIZ_NOT_FOUND")
        if (ownerId != teacherId) {
            throw ForbiddenException("You don't own this quiz", "NOT_QUIZ_OWNER")
        }
    }

    private suspend fun verifyQuestionOwnership(quizId: String, questionId: String) {
        val question = quizRepository.getQuestion(questionId) ?: throw NotFoundException("Question not found", "QUESTION_NOT_FOUND")
        if (question.quizId != quizId) {
            throw NotFoundException("Question does not belong to this quiz", "QUESTION_NOT_IN_QUIZ")
        }
    }

    private fun validateQuizDto(title: String?, timeLimitMinutes: Int?) {
        if (title != null && title.isBlank()) {
            throw BadRequestException("Title must not be blank", "INVALID_INPUT")
        }
        if (timeLimitMinutes != null && timeLimitMinutes <= 0) {
            throw BadRequestException("Time limit must be positive", "INVALID_INPUT")
        }
    }

    private fun validateCreateQuestionDto(dto: CreateQuestionDto) {
        if (dto.prompt.isBlank()) throw BadRequestException("Prompt must not be blank", "INVALID_INPUT")
        if (dto.options.isEmpty()) throw BadRequestException("Options must not be empty", "INVALID_INPUT")
        if (dto.points <= 0) throw BadRequestException("Points must be positive", "INVALID_INPUT")
        if (dto.correctAnswer !in dto.options) {
            throw BadRequestException("correctAnswer must be one of the provided options", "INVALID_INPUT")
        }
    }

    private fun validateUpdateQuestionDto(dto: UpdateQuestionDto) {
        dto.points?.let {
            if (it <= 0) throw BadRequestException("Points must be positive", "INVALID_INPUT")
        }
        dto.prompt?.let {
            if (it.isBlank()) throw BadRequestException("Prompt must not be blank", "INVALID_INPUT")
        }
        dto.options?.let { options ->
            if (options.isEmpty()) throw BadRequestException("Options must not be empty", "INVALID_INPUT")
            dto.correctAnswer?.let { answer ->
                if (answer !in options) throw BadRequestException("correctAnswer must be one of the provided options", "INVALID_INPUT")
            }
        }
    }
}
