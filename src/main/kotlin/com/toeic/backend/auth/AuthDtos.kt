package com.toeic.backend.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val role: String // "teacher" or "student"
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val user: UserInfo
)

@Serializable
data class UserInfo(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String
)
