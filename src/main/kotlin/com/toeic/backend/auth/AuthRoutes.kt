package com.toeic.backend.auth

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = authService.login(request)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = authService.register(request)
            call.respond(HttpStatusCode.Created, response)
        }
    }
}
