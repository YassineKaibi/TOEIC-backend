package com.toeic.backend.submissions

import com.toeic.backend.common.BadRequestException
import com.toeic.backend.common.requireRole
import com.toeic.backend.common.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.submissionRoutes(submissionService: SubmissionService) {
    route("/quizzes/{quizId}/submissions") {
        post {
            call.requireRole("student")
            val quizId = call.parameters["quizId"]
                ?: throw BadRequestException("Missing quizId", "MISSING_PARAM")
            val dto = call.receive<QuizSubmissionDto>()
            val result = submissionService.submit(quizId, call.userId(), dto)
            call.respond(HttpStatusCode.Created, result)
        }
    }
}
