import httpx
import pytest

from toeic_client import (
    ApiError,
    AuthError,
    ClassStudent,
    NotFoundError,
    ToeicClient,
    StudentAnswer,
)


class TestGetClassStudents:
    def test_returns_students(self, client, httpx_mock):
        httpx_mock.add_response(json={
            "items": [
                {
                    "id": "s1",
                    "fullName": "Alice Ben",
                    "email": "alice@example.com",
                    "joinedAt": "2026-04-01T10:00:00",
                },
            ]
        })
        students = client.get_class_students("cls_1")
        assert len(students) == 1
        assert students[0].full_name == "Alice Ben"
        assert students[0].id == "s1"

    def test_returns_empty_list_on_204(self, client, httpx_mock):
        httpx_mock.add_response(status_code=204)
        assert client.get_class_students("cls_1") == []

    def test_raises_not_found(self, client, httpx_mock):
        httpx_mock.add_response(
            status_code=404,
            json={"message": "Class not found", "errorCode": "CLASS_NOT_FOUND"},
        )
        with pytest.raises(NotFoundError) as exc_info:
            client.get_class_students("cls_999")
        assert exc_info.value.error_code == "CLASS_NOT_FOUND"

    def test_raises_auth_error_on_403(self, client, httpx_mock):
        httpx_mock.add_response(
            status_code=403,
            json={"message": "You don't own this class", "errorCode": "NOT_CLASS_OWNER"},
        )
        with pytest.raises(AuthError) as exc_info:
            client.get_class_students("cls_1")
        assert exc_info.value.status_code == 403


class TestHandleResponse:
    def test_non_json_error_body(self, client, httpx_mock):
        httpx_mock.add_response(status_code=502, text="<html>Bad Gateway</html>")
        with pytest.raises(ApiError) as exc_info:
            client.get_class_students("cls_1")
        assert exc_info.value.status_code == 502
        assert exc_info.value.error_code == "UNKNOWN"


class TestSprint2Stubs:
    def test_get_exam_not_implemented(self, client):
        with pytest.raises(NotImplementedError):
            client.get_exam("exam_1")

    def test_submit_grading_not_implemented(self, client):
        with pytest.raises(NotImplementedError):
            client.submit_grading("exam_1", None)


class TestLogin:
    def test_returns_login_response(self, client, httpx_mock):
        httpx_mock.add_response(json={
            "accessToken": "tok_abc",
            "user": {
                "id": "usr_1",
                "fullName": "Alice Ben",
                "email": "alice@example.com",
                "role": "student",
            },
        })
        result = client.login("alice@example.com", "secret")
        assert result.access_token == "tok_abc"
        assert result.user.role == "student"
        assert result.user.full_name == "Alice Ben"

    def test_raises_auth_error_on_401(self, client, httpx_mock):
        httpx_mock.add_response(
            status_code=401,
            json={"message": "Bad credentials", "errorCode": "INVALID_CREDENTIALS"},
        )
        with pytest.raises(AuthError) as exc_info:
            client.login("bad@example.com", "wrong")
        assert exc_info.value.error_code == "INVALID_CREDENTIALS"


class TestGetTeacherClasses:
    def test_returns_classes(self, client, httpx_mock):
        httpx_mock.add_response(json={
            "items": [
                {"id": "cls_1", "name": "Advanced", "studentsCount": 5, "joinCode": "ABCD"},
            ]
        })
        classes = client.get_teacher_classes()
        assert len(classes) == 1
        assert classes[0].join_code == "ABCD"
        assert classes[0].students_count == 5

    def test_returns_empty_list_on_204(self, client, httpx_mock):
        httpx_mock.add_response(status_code=204)
        assert client.get_teacher_classes() == []


class TestGetStudentClasses:
    def test_returns_classes(self, client, httpx_mock):
        httpx_mock.add_response(json={
            "items": [
                {
                    "id": "cls_1",
                    "name": "Advanced",
                    "teacher": {"fullName": "Prof. Mondher"},
                    "joinedAt": "2026-03-01T00:00:00Z",
                },
            ]
        })
        classes = client.get_student_classes()
        assert len(classes) == 1
        assert classes[0].teacher.full_name == "Prof. Mondher"

    def test_returns_empty_list_on_204(self, client, httpx_mock):
        httpx_mock.add_response(status_code=204)
        assert client.get_student_classes() == []


class TestLeaveClass:
    def test_succeeds_on_204(self, client, httpx_mock):
        httpx_mock.add_response(status_code=204)
        client.leave_class("cls_1")  # must not raise

    def test_raises_not_found(self, client, httpx_mock):
        httpx_mock.add_response(
            status_code=404,
            json={"message": "Class not found", "errorCode": "CLASS_NOT_FOUND"},
        )
        with pytest.raises(NotFoundError):
            client.leave_class("cls_999")


class TestGetTeacherQuizzes:
    def test_returns_quizzes(self, client, httpx_mock):
        httpx_mock.add_response(json={
            "items": [
                {
                    "id": "qz_1",
                    "title": "Part 5",
                    "description": None,
                    "timeLimitMinutes": 30,
                    "teacherId": "usr_t1",
                    "createdAt": "2026-01-01T00:00:00Z",
                    "questions": None,
                },
            ]
        })
        quizzes = client.get_teacher_quizzes()
        assert len(quizzes) == 1
        assert quizzes[0].title == "Part 5"

    def test_returns_empty_list_on_204(self, client, httpx_mock):
        httpx_mock.add_response(status_code=204)
        assert client.get_teacher_quizzes() == []


class TestGetQuiz:
    def test_returns_quiz_with_questions(self, client, httpx_mock):
        httpx_mock.add_response(json={
            "id": "qz_1",
            "title": "Part 5",
            "description": "Grammar focus",
            "timeLimitMinutes": 30,
            "teacherId": "usr_t1",
            "createdAt": "2026-01-01T00:00:00Z",
            "questions": [
                {
                    "id": "qq_1",
                    "quizId": "qz_1",
                    "prompt": "Fill in the blank.",
                    "order": 1,
                    "points": 1.0,
                    "options": ["A", "B", "C", "D"],
                }
            ],
        })
        quiz = client.get_quiz("qz_1")
        assert quiz.id == "qz_1"
        assert len(quiz.questions) == 1
        assert quiz.questions[0].prompt == "Fill in the blank."

    def test_raises_not_found(self, client, httpx_mock):
        httpx_mock.add_response(
            status_code=404,
            json={"message": "Quiz not found", "errorCode": "QUIZ_NOT_FOUND"},
        )
        with pytest.raises(NotFoundError):
            client.get_quiz("qz_999")


class TestSubmitQuiz:
    def test_returns_submission_response(self, client, httpx_mock):
        httpx_mock.add_response(
            status_code=201,
            json={"submissionId": "sub_abc", "status": "pending"},
        )
        result = client.submit_quiz(
            quiz_id="qz_1",
            student_id="usr_s1",
            duration_seconds=120,
            answers=[StudentAnswer(question_id="qq_1", selected_option="A")],
        )
        assert result.submission_id == "sub_abc"
        assert result.status == "pending"

    def test_raises_not_found_for_unknown_quiz(self, client, httpx_mock):
        httpx_mock.add_response(
            status_code=404,
            json={"message": "Quiz not found", "errorCode": "QUIZ_NOT_FOUND"},
        )
        with pytest.raises(NotFoundError):
            client.submit_quiz("qz_999", "usr_s1", 60, [])


class TestGetRecommendations:
    def test_returns_recommendations(self, client, httpx_mock):
        httpx_mock.add_response(json={
            "quizzes": [
                {"quizId": "qz_1", "title": "Part 5", "difficulty": None},
            ]
        })
        result = client.get_recommendations("usr_s1")
        assert len(result.quizzes) == 1
        assert result.quizzes[0].quiz_id == "qz_1"

    def test_returns_empty_quizzes_list(self, client, httpx_mock):
        httpx_mock.add_response(json={"quizzes": []})
        result = client.get_recommendations("usr_s1")
        assert result.quizzes == []

    def test_raises_auth_error_on_403(self, client, httpx_mock):
        httpx_mock.add_response(
            status_code=403,
            json={"message": "Student mismatch", "errorCode": "STUDENT_MISMATCH"},
        )
        with pytest.raises(AuthError):
            client.get_recommendations("usr_other")
