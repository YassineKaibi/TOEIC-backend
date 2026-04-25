package com.toeic.backend.recommendations

import com.toeic.backend.common.BadRequestException
import com.toeic.backend.common.ForbiddenException
import com.toeic.backend.common.Role
import com.toeic.backend.common.requireRole
import com.toeic.backend.common.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.recommendationRoutes(service: RecommendationService) {
    route("/students/{studentId}/recommendations") {
        get {
            call.requireRole(Role.STUDENT)
            val studentId = call.parameters["studentId"]
                ?: throw BadRequestException("Missing studentId", "MISSING_PARAM")
            if (studentId != call.userId()) {
                throw ForbiddenException("Cannot read another student's recommendations", "STUDENT_MISMATCH")
            }
            val result = service.getFor(studentId)
            call.respond(HttpStatusCode.OK, result)
        }
    }
}
