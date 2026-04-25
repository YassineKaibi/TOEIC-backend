package com.toeic.backend.submissions

import com.toeic.backend.ai.AiClient
import com.toeic.backend.ai.AiQuizResultRequest
import com.toeic.backend.common.AppException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class SubmissionDispatcher(
    private val submissionRepository: SubmissionRepository,
    private val aiClient: AiClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val idleTimeoutMs: Long = 30_000L
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val perStudent = ConcurrentHashMap<String, Channel<AiQuizResultRequest>>()

    fun dispatch(payload: AiQuizResultRequest) {
        val ch = perStudent.computeIfAbsent(payload.studentId) { studentId ->
            val newCh = Channel<AiQuizResultRequest>(capacity = Channel.UNLIMITED)
            scope.launch { processQueue(studentId, newCh) }
            newCh
        }
        ch.trySend(payload) // UNLIMITED → never fails
    }

    // Exposed for testing only
    fun channelCount(): Int = perStudent.size

    private suspend fun processQueue(studentId: String, ch: ReceiveChannel<AiQuizResultRequest>) {
        try {
            while (true) {
                val payload = withTimeoutOrNull(idleTimeoutMs) { ch.receive() } ?: break
                try {
                    aiClient.postQuizResult(payload)
                    submissionRepository.updateAiStatus(payload.submissionId, "received")
                } catch (e: AppException) {
                    logger.warn("AI dispatch failed for submission ${payload.submissionId}: ${e.message}")
                    submissionRepository.updateAiStatus(payload.submissionId, "failed")
                } catch (e: Exception) {
                    logger.error("Unexpected AI dispatch error for ${payload.submissionId}", e)
                    submissionRepository.updateAiStatus(payload.submissionId, "failed")
                }
            }
        } finally {
            perStudent.remove(studentId, ch)
            (ch as Channel).cancel()
        }
    }
}
