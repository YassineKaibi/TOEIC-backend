package com.toeic.backend.quizzes

import com.toeic.backend.common.AppException
import com.toeic.backend.common.BadRequestException
import com.toeic.backend.common.requireRole
import com.toeic.backend.common.respondList
import com.toeic.backend.common.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.quizRoutes(quizService: QuizService) {
    route("/teacher/quizzes") {
        
        post {
            call.requireRole("teacher")
            val dto = call.receive<CreateQuizDto>()
            val result = quizService.createQuiz(call.userId(), dto)
            call.respond(HttpStatusCode.Created, result)
        }

        get {
            call.requireRole("teacher")
            val quizzes = quizService.getTeacherQuizzes(call.userId())
            call.respondList(quizzes)
        }

        route("/{quizId}") {
            get {
                call.requireRole("teacher")
                val quizId = call.parameters["quizId"] ?: throw BadRequestException("Missing quizId", "MISSING_PARAM")
                val quiz = quizService.getQuiz(quizId, call.userId())
                call.respond(HttpStatusCode.OK, quiz)
            }

            patch {
                call.requireRole("teacher")
                val quizId = call.parameters["quizId"] ?: throw BadRequestException("Missing quizId", "MISSING_PARAM")
                val dto = call.receive<UpdateQuizDto>()
                val result = quizService.updateQuiz(quizId, call.userId(), dto)
                call.respond(HttpStatusCode.OK, result)
            }

            delete {
                call.requireRole("teacher")
                val quizId = call.parameters["quizId"] ?: throw BadRequestException("Missing quizId", "MISSING_PARAM")
                quizService.archiveQuiz(quizId, call.userId())
                call.respond(HttpStatusCode.NoContent)
            }

            route("/questions") {
                post {
                    call.requireRole("teacher")
                    val quizId = call.parameters["quizId"] ?: throw BadRequestException("Missing quizId", "MISSING_PARAM")
                    val dto = call.receive<CreateQuestionDto>()
                    val result = quizService.createQuestion(quizId, call.userId(), dto)
                    call.respond(HttpStatusCode.Created, result)
                }

                route("/{questionId}") {
                    patch {
                        call.requireRole("teacher")
                        val quizId = call.parameters["quizId"] ?: throw BadRequestException("Missing quizId", "MISSING_PARAM")
                        val questionId = call.parameters["questionId"] ?: throw BadRequestException("Missing questionId", "MISSING_PARAM")
                        val dto = call.receive<UpdateQuestionDto>()
                        val result = quizService.updateQuestion(quizId, questionId, call.userId(), dto)
                        call.respond(HttpStatusCode.OK, result)
                    }

                    delete {
                        call.requireRole("teacher")
                        val quizId = call.parameters["quizId"] ?: throw BadRequestException("Missing quizId", "MISSING_PARAM")
                        val questionId = call.parameters["questionId"] ?: throw BadRequestException("Missing questionId", "MISSING_PARAM")
                        quizService.deleteQuestion(quizId, questionId, call.userId())
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}
