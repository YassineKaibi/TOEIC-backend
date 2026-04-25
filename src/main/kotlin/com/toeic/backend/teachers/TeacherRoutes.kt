package com.toeic.backend.teachers

import com.toeic.backend.classes.ClassService
import com.toeic.backend.common.Role
import com.toeic.backend.common.requireRole
import com.toeic.backend.common.respondList
import com.toeic.backend.common.userId
import io.ktor.server.routing.*

fun Route.teacherRoutes(classService: ClassService) {
    route("/teachers/me") {
        get("/classes") {
            call.requireRole(Role.TEACHER)
            val classes = classService.getTeacherClasses(call.userId())
            call.respondList(classes)
        }
    }
}
