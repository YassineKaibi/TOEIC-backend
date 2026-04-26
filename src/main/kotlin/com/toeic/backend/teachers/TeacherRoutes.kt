package com.toeic.backend.teachers

import com.toeic.backend.classes.ClassService
import com.toeic.backend.common.Role
import com.toeic.backend.common.requireRole
import com.toeic.backend.common.respondPaged
import com.toeic.backend.common.userId
import io.ktor.server.routing.*

fun Route.teacherRoutes(classService: ClassService) {
    route("/teachers/me") {
        get("/classes") {
            call.requireRole(Role.TEACHER)
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = (call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
            val (items, total) = classService.getTeacherClasses(call.userId(), page, pageSize)
            call.respondPaged(items, total, page, pageSize)
        }
    }
}
