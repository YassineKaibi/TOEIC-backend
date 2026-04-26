package com.toeic.backend.classes

import com.toeic.backend.common.Role
import com.toeic.backend.common.requireRole
import com.toeic.backend.common.respondPaged
import com.toeic.backend.common.userId
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.classRoutes(classService: ClassService) {
    route("/classes") {
        post {
            call.requireRole(Role.TEACHER)
            val request = call.receive<CreateClassRequest>()
            val response = classService.createClass(call.userId(), request)
            call.respond(HttpStatusCode.Created, response)
        }

        post("/join") {
            call.requireRole(Role.STUDENT)
            val request = call.receive<JoinClassRequest>()
            classService.joinClass(call.userId(), request)
            call.respond(HttpStatusCode.OK)
        }

        get("/{classId}/students") {
            call.requireRole(Role.TEACHER)
            val classId = call.parameters["classId"]!!
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = (call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
            val (students, total) = classService.getClassStudents(call.userId(), classId, page, pageSize)
            call.respondPaged(students, total, page, pageSize)
        }

        delete("/{classId}/students/{studentId}") {
            call.requireRole(Role.TEACHER)
            val classId = call.parameters["classId"]!!
            val studentId = call.parameters["studentId"]!!
            classService.removeStudent(call.userId(), classId, studentId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
