package com.toeic.backend

import com.toeic.backend.ai.AiQuizResultRequest
import com.toeic.backend.submissions.SubmissionDispatcher
import com.toeic.backend.submissions.SubmissionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubmissionDispatcherTest {

    @Test
    fun dispatch_serializes_calls_for_same_student() = runBlocking {
        val aiClient = FakeAiClient()
        val submissionRepository = object : SubmissionRepository() {
            override suspend fun updateAiStatus(submissionId: String, status: String) = Unit
        }
        val dispatcher = SubmissionDispatcher(submissionRepository, aiClient)
        
        // Submit 5 payloads for the same student
        repeat(5) { i ->
            val payload = AiQuizResultRequest(
                submissionId = "sub-$i",
                studentId = "student-01",
                quizId = "quiz-01",
                score = 1.0,
                maxScore = 2.0,
                correctCount = 1,
                totalCount = 2,
                durationSeconds = 60,
                answers = emptyList(),
                submittedAt = Clock.System.now().toString()
            )
            dispatcher.dispatch(payload)
        }
        
        // Wait for all to process
        delay(2000)
        
        // Verify they were called in order
        assertEquals(listOf("sub-0", "sub-1", "sub-2", "sub-3", "sub-4"), aiClient.callOrder)
    }

    @Test
    fun dispatch_runs_in_parallel_for_different_students() = runBlocking {
        val aiClient = FakeAiClient(delayMs = 100)
        val submissionRepository = object : SubmissionRepository() {
            override suspend fun updateAiStatus(submissionId: String, status: String) = Unit
        }
        val dispatcher = SubmissionDispatcher(submissionRepository, aiClient)
        
        val startTime = System.currentTimeMillis()
        
        // Submit one payload each for two students
        val payload1 = AiQuizResultRequest(
            submissionId = "sub-s1",
            studentId = "student-01",
            quizId = "quiz-01",
            score = 1.0,
            maxScore = 2.0,
            correctCount = 1,
            totalCount = 2,
            durationSeconds = 60,
            answers = emptyList(),
            submittedAt = Clock.System.now().toString()
        )
        
        val payload2 = AiQuizResultRequest(
            submissionId = "sub-s2",
            studentId = "student-02",
            quizId = "quiz-01",
            score = 1.0,
            maxScore = 2.0,
            correctCount = 1,
            totalCount = 2,
            durationSeconds = 60,
            answers = emptyList(),
            submittedAt = Clock.System.now().toString()
        )
        
        dispatcher.dispatch(payload1)
        dispatcher.dispatch(payload2)

        // Poll until both calls complete (up to 500ms), then measure elapsed
        repeat(50) {
            if (aiClient.callOrder.size >= 2) return@repeat
            delay(10)
        }
        val elapsed = System.currentTimeMillis() - startTime

        // Should run in parallel: ~100ms, not sequentially: ~200ms
        assertTrue(elapsed < 250, "Took ${elapsed}ms, expected parallel execution < 250ms")
    }

    @Test
    fun dispatcher_cleans_up_channel_after_idle_timeout() = runBlocking {
        val aiClient = FakeAiClient()
        val submissionRepository = object : SubmissionRepository() {
            override suspend fun updateAiStatus(submissionId: String, status: String) = Unit
        }
        // Use a very short idle timeout for testing (100ms)
        val dispatcher = SubmissionDispatcher(submissionRepository, aiClient, idleTimeoutMs = 100L)

        val payload = AiQuizResultRequest(
            submissionId = "sub-cleanup",
            studentId = "student-cleanup",
            quizId = "quiz-01",
            score = 1.0,
            maxScore = 2.0,
            correctCount = 1,
            totalCount = 2,
            durationSeconds = 60,
            answers = emptyList(),
            submittedAt = Clock.System.now().toString()
        )
        dispatcher.dispatch(payload)

        // Wait for processing + idle timeout + buffer
        delay(500)

        // The channel map should be empty after idle timeout
        assertEquals(0, dispatcher.channelCount())
    }
}
