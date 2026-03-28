package com.toeic.backend.classes

import kotlinx.serialization.Serializable

@Serializable
data class CreateClassRequest(val name: String, val description: String? = null)
@Serializable
data class JoinClassRequest(val code: String)

@Serializable
data class ClassResponse(
    val id: String,
    val name: String,
    val description: String?,
    val joinCode: String,
    val teacherId: String,
    val createdAt: String
)

@Serializable
data class TeacherClassItem(
    val id: String,
    val name: String,
    val studentsCount: Int,
    val joinCode: String
)

@Serializable
data class StudentClassItem(
    val id: String,
    val name: String,
    val teacher: TeacherInfo,
    val joinedAt: String
)

@Serializable
data class TeacherInfo(val fullName: String)

@Serializable
data class ClassStudentItem(
    val id: String,
    val fullName: String,
    val email: String,
    val joinedAt: String
)
