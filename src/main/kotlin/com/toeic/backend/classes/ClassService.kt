package com.toeic.backend.classes

import com.toeic.backend.common.BadRequestException
import com.toeic.backend.common.ConflictException
import com.toeic.backend.common.ForbiddenException
import com.toeic.backend.common.NotFoundException
import com.toeic.backend.enrollments.EnrollmentRepository

class ClassService(
    private val classRepository: ClassRepository,
    private val enrollmentRepository: EnrollmentRepository
) {

    suspend fun createClass(teacherId: String, request: CreateClassRequest): ClassResponse {
        val joinCode = generateJoinCode()
        return classRepository.create(
            name = request.name,
            description = request.description,
            joinCode = joinCode,
            teacherId = teacherId
        )
    }

    suspend fun joinClass(studentId: String, request: JoinClassRequest) {
        val clazz = classRepository.findByJoinCode(request.code)
            ?: throw BadRequestException("Invalid join code", "WRONG_CODE")

        if (enrollmentRepository.exists(clazz.id, studentId)) {
            throw ConflictException("Already joined this class", "ALREADY_JOINED")
        }

        enrollmentRepository.insert(clazz.id, studentId)
    }

    suspend fun getTeacherClasses(teacherId: String): List<TeacherClassItem> {
        val classes = classRepository.findByTeacherId(teacherId)
        return classes.map { clazz ->
            TeacherClassItem(
                id = clazz.id,
                name = clazz.name,
                studentsCount = enrollmentRepository.countByClassId(clazz.id),
                joinCode = clazz.joinCode
            )
        }
    }

    suspend fun getStudentClasses(studentId: String): List<StudentClassItem> {
        return enrollmentRepository.findClassesByStudentId(studentId)
    }

    suspend fun getClassStudents(teacherId: String, classId: String): List<ClassStudentItem> {
        verifyClassOwnership(teacherId, classId)
        return enrollmentRepository.findStudentsByClassId(classId)
    }

    suspend fun removeStudent(teacherId: String, classId: String, studentId: String) {
        verifyClassOwnership(teacherId, classId)
        enrollmentRepository.delete(classId, studentId)
    }

    suspend fun leaveClass(studentId: String, classId: String) {
        val clazz = classRepository.findById(classId)
            ?: throw NotFoundException("Class not found", "CLASS_NOT_FOUND")
        if (!enrollmentRepository.exists(clazz.id, studentId)) {
            throw ForbiddenException("You are not enrolled in this class", "NOT_ENROLLED")
        }
        enrollmentRepository.delete(clazz.id, studentId)
    }

    private suspend fun verifyClassOwnership(teacherId: String, classId: String) {
        val clazz = classRepository.findById(classId)
            ?: throw NotFoundException("Class not found", "CLASS_NOT_FOUND")

        if (clazz.teacherId != teacherId) {
            throw ForbiddenException("You don't own this class", "NOT_CLASS_OWNER")
        }
    }

    private suspend fun generateJoinCode(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz1234567890"
        var unique = false
        var choice = ""
        while (!unique) {
        	choice = (1..6).map { chars.random() }.joinToString("")
        	if (classRepository.findByJoinCode(choice) == null) {
        		unique = true
        	}
        }
        return choice
    }
}
