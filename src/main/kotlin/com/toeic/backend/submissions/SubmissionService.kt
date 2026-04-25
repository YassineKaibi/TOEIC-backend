package com.toeic.backend.submissions

import com.toeic.backend.ai.AiAnswerEntry
import com.toeic.backend.ai.AiQuizResultRequest
import com.toeic.backend.common.BadRequestException
import com.toeic.backend.common.ForbiddenException
import com.toeic.backend.common.NotFoundException
import com.toeic.backend.enrollments.EnrollmentRepository
import com.toeic.backend.quizzes.QuizRepository
import kotlinx.datetime.Clock
import java.util.UUID

class SubmissionService(
    private val quizRepository: QuizRepository,
    private val submissionRepository: SubmissionRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val dispatcher: SubmissionDispatcher
) {
    suspend fun submit(quizId: String, callerId: String, dto: QuizSubmissionDto): QuizSubmissionResponse {
        // 1. validate dto (see §2.1 table)
        if (dto.studentId != callerId)
            throw ForbiddenException("Cannot submit for another student", "STUDENT_MISMATCH")
        if (dto.durationSeconds < 0)
            throw BadRequestException("durationSeconds must be >= 0", "INVALID_INPUT")
        if (dto.answers.isEmpty())
            throw BadRequestException("answers must not be empty", "INVALID_INPUT")
        dto.answers.forEach {
            if (it.questionId.isBlank() || it.selectedOption.isBlank())
                throw BadRequestException("answer fields must not be blank", "INVALID_INPUT")
        }
        if (dto.answers.map { it.questionId }.toSet().size != dto.answers.size)
            throw BadRequestException("duplicate questionId in answers", "DUPLICATE_ANSWER")

        // 2. load quiz + questions
        val quiz = quizRepository.getQuiz(quizId)
            ?: throw NotFoundException("Quiz not found", "QUIZ_NOT_FOUND")
        val questions = quiz.questions ?: emptyList()
        val questionMap = questions.associateBy { it.id }

        // 3. authorize via enrollment
        val classIds = quizRepository.getClassIdsForQuiz(quizId)
        val enrolled = classIds.any { enrollmentRepository.exists(it, callerId) }
        if (!enrolled) throw ForbiddenException(
            "Student is not enrolled in any class assigned to this quiz",
            "NOT_ENROLLED_IN_QUIZ_CLASS"
        )

        // 4. validate every answer's questionId belongs to the quiz
        dto.answers.forEach {
            if (it.questionId !in questionMap)
                throw BadRequestException("question ${it.questionId} not in quiz", "QUESTION_NOT_IN_QUIZ")
        }

        // 5. grade
        val totalCount = questions.size
        val maxScore = questions.sumOf { it.points }
        var correctCount = 0
        var score = 0.0
        val gradedEntries = mutableListOf<AiAnswerEntry>()
        val answersByQid = dto.answers.associateBy { it.questionId }

        for (q in questions) {
            val ans = answersByQid[q.id]
            val correct = ans != null && ans.selectedOption == q.correctAnswer
            val pts = if (correct) q.points else 0.0
            if (correct) correctCount++
            score += pts
            if (ans != null) {
                gradedEntries.add(AiAnswerEntry(
                    questionId = q.id,
                    selectedOption = ans.selectedOption,
                    correctOption = q.correctAnswer,
                    correct = correct,
                    points = pts
                ))
            }
        }

        // 6. persist
        val submissionId = UUID.randomUUID().toString()
        val submittedAt = Clock.System.now()
        val graded = GradedSubmission(
            submissionId, quizId, callerId, score, maxScore,
            correctCount, totalCount, dto.durationSeconds, dto.answers,
            gradedEntries, submittedAt
        )
        submissionRepository.insert(graded)

        // 7. fire AI dispatch (non-blocking; channel-backed, sequential per student)
        dispatcher.dispatch(AiQuizResultRequest(
            submissionId = submissionId,
            studentId = callerId,
            quizId = quizId,
            score = score,
            maxScore = maxScore,
            correctCount = correctCount,
            totalCount = totalCount,
            durationSeconds = dto.durationSeconds,
            answers = gradedEntries,
            submittedAt = submittedAt.toString()
        ))

        // 8. respond
        return QuizSubmissionResponse(submissionId = submissionId, status = "received")
    }
}
