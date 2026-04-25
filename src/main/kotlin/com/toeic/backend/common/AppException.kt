package com.toeic.backend.common

import io.ktor.http.*

open class AppException(
    val statusCode: HttpStatusCode,
    override val message: String,
    val errorCode: String
) : RuntimeException(message)

class UnauthorizedException(
    message: String = "Invalid credentials",
    errorCode: String = "INVALID_CREDENTIALS"
) : AppException(HttpStatusCode.Unauthorized, message, errorCode)

class ForbiddenException(
    message: String = "Insufficient permissions",
    errorCode: String = "TEACHER_ROLE_REQUIRED"
) : AppException(HttpStatusCode.Forbidden, message, errorCode)

class NotFoundException(
    message: String = "Resource not found",
    errorCode: String = "CLASS_NOT_FOUND"
) : AppException(HttpStatusCode.NotFound, message, errorCode)

class ConflictException(
    message: String = "Conflict",
    errorCode: String = "ALREADY_JOINED"
) : AppException(HttpStatusCode.Conflict, message, errorCode)

class BadRequestException(
    message: String = "Bad request",
    errorCode: String = "WRONG_CODE"
) : AppException(HttpStatusCode.BadRequest, message, errorCode)

class BadGatewayException(
    message: String = "Upstream service unavailable",
    errorCode: String = "AI_UNAVAILABLE"
) : AppException(HttpStatusCode.BadGateway, message, errorCode)
