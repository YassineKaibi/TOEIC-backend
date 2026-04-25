package com.toeic.backend.submissions

import com.toeic.backend.db.dbQuery
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update

open class SubmissionRepository {

    open suspend fun insert(graded: GradedSubmission) {
        dbQuery {
            SubmissionsTable.insert {
                it[id] = graded.submissionId
                it[quizId] = graded.quizId
                it[studentId] = graded.studentId
                it[durationSeconds] = graded.durationSeconds
                it[score] = graded.score
                it[maxScore] = graded.maxScore
                it[correctCount] = graded.correctCount
                it[totalCount] = graded.totalCount
                it[answersJson] = Json.encodeToString(graded.answers)
                it[aiDispatchStatus] = "pending"
                it[createdAt] = graded.submittedAt
            }
        }
    }

    open suspend fun updateAiStatus(submissionId: String, status: String) {
        dbQuery {
            SubmissionsTable.update({ SubmissionsTable.id eq submissionId }) {
                it[aiDispatchStatus] = status
            }
        }
    }
}
