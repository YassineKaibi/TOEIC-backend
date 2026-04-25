import pytest

from toeic_client import (
    AsyncToeicClient,
    AuthError,
    NotFoundError,
    StudentAnswer,
)


@pytest.mark.anyio
async def test_login(async_client, httpx_mock):
    httpx_mock.add_response(json={
        "accessToken": "tok_abc",
        "user": {
            "id": "usr_1",
            "fullName": "Alice Ben",
            "email": "alice@example.com",
            "role": "student",
        },
    })
    result = await async_client.login("alice@example.com", "secret")
    assert result.access_token == "tok_abc"


@pytest.mark.anyio
async def test_get_teacher_classes(async_client, httpx_mock):
    httpx_mock.add_response(json={
        "items": [
            {"id": "cls_1", "name": "Adv", "studentsCount": 3, "joinCode": "XYZ"},
        ]
    })
    classes = await async_client.get_teacher_classes()
    assert classes[0].id == "cls_1"


@pytest.mark.anyio
async def test_get_student_classes_empty(async_client, httpx_mock):
    httpx_mock.add_response(status_code=204)
    classes = await async_client.get_student_classes()
    assert classes == []


@pytest.mark.anyio
async def test_get_teacher_quizzes(async_client, httpx_mock):
    httpx_mock.add_response(json={
        "items": [
            {
                "id": "qz_1",
                "title": "Part 5",
                "description": None,
                "timeLimitMinutes": None,
                "teacherId": "usr_t1",
                "createdAt": "2026-01-01T00:00:00Z",
                "questions": None,
            }
        ]
    })
    quizzes = await async_client.get_teacher_quizzes()
    assert quizzes[0].title == "Part 5"


@pytest.mark.anyio
async def test_submit_quiz(async_client, httpx_mock):
    httpx_mock.add_response(
        status_code=201,
        json={"submissionId": "sub_1", "status": "pending"},
    )
    result = await async_client.submit_quiz(
        quiz_id="qz_1",
        student_id="usr_s1",
        duration_seconds=90,
        answers=[StudentAnswer(question_id="qq_1", selected_option="B")],
    )
    assert result.submission_id == "sub_1"


@pytest.mark.anyio
async def test_get_recommendations(async_client, httpx_mock):
    httpx_mock.add_response(json={
        "quizzes": [{"quizId": "qz_2", "title": "Part 6", "difficulty": None}]
    })
    result = await async_client.get_recommendations("usr_s1")
    assert result.quizzes[0].quiz_id == "qz_2"


@pytest.mark.anyio
async def test_async_sprint2_stubs_raise(async_client):
    with pytest.raises(NotImplementedError):
        await async_client.get_exam("exam_1")
    with pytest.raises(NotImplementedError):
        await async_client.submit_grading("exam_1", None)
