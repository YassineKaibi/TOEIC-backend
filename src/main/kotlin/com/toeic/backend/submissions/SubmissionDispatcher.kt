package com.toeic.backend.submissions

import com.toeic.backend.ai.AiClient
import com.toeic.backend.ai.AiQuizResultRequest
import com.toeic.backend.common.AppException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class SubmissionDispatcher(
    private val submissionRepository: SubmissionRepository,
    private val aiClient: AiClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val perStudent = ConcurrentHashMap<String, Channel<AiQuizResultRequest>>() // TODO(reap): channels are leaked

    fun dispatch(payload: AiQuizResultRequest) {
        val ch = perStudent.computeIfAbsent(payload.studentId) { studentId ->
            val newCh = Channel<AiQuizResultRequest>(capacity = Channel.UNLIMITED)
            scope.launch { processQueue(studentId, newCh) }
            newCh
        }
        ch.trySend(payload) // UNLIMITED → never fails
    }

    private suspend fun processQueue(studentId: String, ch: Channel<AiQuizResultRequest>) {
        for (payload in ch) {
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
    }
}
