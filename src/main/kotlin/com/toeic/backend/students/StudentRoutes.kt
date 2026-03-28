package com.toeic.backend.students

import com.toeic.backend.classes.ClassService
import com.toeic.backend.common.requireRole
import com.toeic.backend.common.respondList
import com.toeic.backend.common.userId
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentRoutes(classService: ClassService) {
    route("/students/me") {
        get("/classes") {
            call.requireRole("student")
            val classes = classService.getStudentClasses(call.userId())
            call.respondList(classes)
        }

        delete("/classes/{classId}") {
            call.requireRole("student")
            val classId = call.parameters["classId"]!!
            classService.leaveClass(call.userId(), classId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
