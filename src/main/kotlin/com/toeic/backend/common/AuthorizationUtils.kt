package com.toeic.backend.common

import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*

fun RoutingCall.userId(): String =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
        ?: throw UnauthorizedException("Missing user ID in token")

fun RoutingCall.userRole(): String =
    principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
        ?: throw UnauthorizedException("Missing role in token")

fun RoutingCall.requireRole(role: Role) {
    val actual = try {
        Role.fromString(userRole())
    } catch (e: BadRequestException) {
        throw ForbiddenException("Invalid role in token", "INVALID_ROLE")
    }
    if (actual != role) {
        throw ForbiddenException(
            message = "Role '${role.value}' required",
            errorCode = "${role.name}_ROLE_REQUIRED"
        )
    }
}
