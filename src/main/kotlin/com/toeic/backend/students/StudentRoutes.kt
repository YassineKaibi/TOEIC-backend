package com.toeic.backend.students

import com.toeic.backend.classes.ClassService
import com.toeic.backend.common.Role
import com.toeic.backend.common.requireRole
import com.toeic.backend.common.respondPaged
import com.toeic.backend.common.userId
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentRoutes(classService: ClassService) {
    route("/students/me") {
        get("/classes") {
            call.requireRole(Role.STUDENT)
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = (call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
            val (items, total) = classService.getStudentClasses(call.userId(), page, pageSize)
            call.respondPaged(items, total, page, pageSize)
        }

        delete("/classes/{classId}") {
            call.requireRole(Role.STUDENT)
            val classId = call.parameters["classId"]!!
            classService.leaveClass(call.userId(), classId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
