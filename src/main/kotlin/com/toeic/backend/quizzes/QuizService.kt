package com.toeic.backend.quizzes

import com.toeic.backend.common.ForbiddenException
import com.toeic.backend.common.NotFoundException

class QuizService(private val quizRepository: QuizRepository) {

    suspend fun createQuiz(teacherId: String, dto: CreateQuizDto): QuizResponse {
        return quizRepository.createQuiz(teacherId, dto)
    }

    suspend fun getTeacherQuizzes(teacherId: String): List<QuizResponse> {
        return quizRepository.getTeacherQuizzes(teacherId)
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
        return quizRepository.updateQuiz(quizId, dto) ?: throw NotFoundException("Quiz not found", "QUIZ_NOT_FOUND")
    }

    suspend fun archiveQuiz(quizId: String, teacherId: String) {
        verifyOwnership(quizId, teacherId)
        val success = quizRepository.archiveQuiz(quizId)
        if (!success) throw NotFoundException("Quiz not found", "QUIZ_NOT_FOUND")
    }

    suspend fun createQuestion(quizId: String, teacherId: String, dto: CreateQuestionDto): QuestionResponse {
        verifyOwnership(quizId, teacherId)
        return quizRepository.createQuestion(quizId, dto)
    }

    suspend fun updateQuestion(quizId: String, questionId: String, teacherId: String, dto: UpdateQuestionDto): QuestionResponse {
        verifyOwnership(quizId, teacherId)
        verifyQuestionOwnership(quizId, questionId)
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
}
