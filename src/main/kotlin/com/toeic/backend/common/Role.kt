package com.toeic.backend.common

enum class Role(val value: String) {
    TEACHER("teacher"),
    STUDENT("student");

    companion object {
        fun fromString(s: String): Role =
            entries.find { it.value == s }
                ?: throw BadRequestException("Invalid role: $s", "INVALID_ROLE")
    }
}
