package com.toeic.backend.quizzes

import com.toeic.backend.common.BadRequestException
import com.toeic.backend.common.Role
import com.toeic.backend.common.requireRole
import com.toeic.backend.common.respondList
import com.toeic.backend.common.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.quizRoutes(quizService: QuizService) {
    route("/teachers/me/quizzes") {

        post {
            call.requireRole(Role.TEACHER)
            val dto = call.receive<CreateQuizDto>()
            val result = quizService.createQuiz(call.userId(), dto)
            call.respond(HttpStatusCode.Created, result)
        }

        get {
            call.requireRole(Role.TEACHER)
            val quizzes = quizService.getTeacherQuizzes(call.userId())
            call.respondList(quizzes)
        }

        route("/{quizId}") {
            get {
                call.requireRole(Role.TEACHER)
                val quizId = call.requireParam("quizId")
                val quiz = quizService.getQuiz(quizId, call.userId())
                call.respond(HttpStatusCode.OK, quiz)
            }

            patch {
                call.requireRole(Role.TEACHER)
                val quizId = call.requireParam("quizId")
                val dto = call.receive<UpdateQuizDto>()
                val result = quizService.updateQuiz(quizId, call.userId(), dto)
                call.respond(HttpStatusCode.OK, result)
            }

            delete {
                call.requireRole(Role.TEACHER)
                val quizId = call.requireParam("quizId")
                quizService.archiveQuiz(quizId, call.userId())
                call.respond(HttpStatusCode.NoContent)
            }

            route("/questions") {
                post {
                    call.requireRole(Role.TEACHER)
                    val quizId = call.requireParam("quizId")
                    val dto = call.receive<CreateQuestionDto>()
                    val result = quizService.createQuestion(quizId, call.userId(), dto)
                    call.respond(HttpStatusCode.Created, result)
                }

                route("/{questionId}") {
                    patch {
                        call.requireRole(Role.TEACHER)
                        val quizId = call.requireParam("quizId")
                        val questionId = call.requireParam("questionId")
                        val dto = call.receive<UpdateQuestionDto>()
                        val result = quizService.updateQuestion(quizId, questionId, call.userId(), dto)
                        call.respond(HttpStatusCode.OK, result)
                    }

                    delete {
                        call.requireRole(Role.TEACHER)
                        val quizId = call.requireParam("quizId")
                        val questionId = call.requireParam("questionId")
                        quizService.deleteQuestion(quizId, questionId, call.userId())
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}

private fun RoutingCall.requireParam(name: String): String =
    parameters[name] ?: throw BadRequestException("Missing $name", "MISSING_PARAM")
