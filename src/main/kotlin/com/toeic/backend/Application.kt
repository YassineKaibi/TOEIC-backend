package com.toeic.backend

import com.toeic.backend.auth.AuthService
import com.toeic.backend.auth.JwtService
import com.toeic.backend.classes.ClassRepository
import com.toeic.backend.classes.ClassService
import com.toeic.backend.db.DatabaseFactory
import com.toeic.backend.enrollments.EnrollmentRepository
import com.toeic.backend.plugins.*
import com.toeic.backend.quizzes.QuizRepository
import com.toeic.backend.quizzes.QuizService
import com.toeic.backend.users.UserRepository
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureStatusPages()
    configureCallLogging()

    val jwtService = JwtService()
    configureAuthentication(jwtService)

    DatabaseFactory.init()

    val userRepository = UserRepository()
    val classRepository = ClassRepository()
    val enrollmentRepository = EnrollmentRepository()
    val quizRepository = QuizRepository()

    val authService = AuthService(userRepository, jwtService)
    val classService = ClassService(classRepository, enrollmentRepository)
    val quizService = QuizService(quizRepository)

    configureRouting(authService, classService, quizService)
}
