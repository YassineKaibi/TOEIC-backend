package com.toeic.backend.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.toeic.backend.common.BadRequestException
import com.toeic.backend.common.ConflictException
import com.toeic.backend.common.UnauthorizedException
import com.toeic.backend.users.UserRepository
import java.util.*

class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {

    suspend fun register(request: RegisterRequest): LoginResponse {
        if (userRepository.findByEmail(request.email) != null) {
            throw ConflictException("Email already registered", "EMAIL_ALREADY_EXISTS")
        }
        if (request.role !in listOf("student", "teacher")) {
            throw BadRequestException("Invalid role", "INVALID_ROLE")
        }
        val id = UUID.randomUUID().toString()
        val fullName = request.fullName
        val email = request.email
        val role = request.role
        val token = jwtService.generateToken(id, request.role)
        val passwordHash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())
        userRepository.insert(id, fullName, email, passwordHash, role)
        return LoginResponse(
            accessToken = token,
            user = UserInfo(
                id = id,
                fullName = fullName,
                email = email,
                role = role
            )
        )
    }

    suspend fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw UnauthorizedException()

        val passwordMatch = BCrypt.verifyer()
            .verify(request.password.toCharArray(), user.passwordHash)
            .verified

        if (!passwordMatch) {
            throw UnauthorizedException()
        }

        val token = jwtService.generateToken(user.id, user.role)

        return LoginResponse(
            accessToken = token,
            user = UserInfo(
                id = user.id,
                fullName = user.fullName,
                email = user.email,
                role = user.role
            )
        )
    }
}
