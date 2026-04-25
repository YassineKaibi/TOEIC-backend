import httpx

from .exceptions import ApiError, AuthError, NotFoundError
from .models import (
    LoginResponse,
    Quiz,
    QuizRecommendation,
    RecommendationsResponse,
    StudentAnswer,
    StudentClassItem,
    SubmitQuizResponse,
    TeacherClassItem,
    ClassStudent,
    Exam,
    ExamStatus,
    GradingResponse,
    GradingResult,
)


class AsyncToeicClient:
    """Async client for the TOEIC Platform API."""

    def __init__(self, base_url: str, token: str, timeout: float = 30.0) -> None:
        self._client = httpx.AsyncClient(
            base_url=base_url,
            headers={"Authorization": f"Bearer {token}"},
            timeout=timeout,
        )

    async def aclose(self) -> None:
        await self._client.aclose()

    async def __aenter__(self) -> "AsyncToeicClient":
        return self

    async def __aexit__(self, *args: object) -> None:
        await self.aclose()

    # ── Auth ──────────────────────────────────────

    async def login(self, email: str, password: str) -> LoginResponse:
        resp = await self._client.post(
            "/auth/login",
            json={"email": email, "password": password},
        )
        self._handle_response(resp)
        return LoginResponse.model_validate(resp.json())

    async def register(
        self,
        full_name: str,
        email: str,
        password: str,
        role: str,
    ) -> LoginResponse:
        resp = await self._client.post(
            "/auth/register",
            json={
                "fullName": full_name,
                "email": email,
                "password": password,
                "role": role,
            },
        )
        self._handle_response(resp)
        return LoginResponse.model_validate(resp.json())

    # ── Classes ───────────────────────────────────

    async def get_class_students(self, class_id: str) -> list[ClassStudent]:
        resp = await self._client.get(f"/classes/{class_id}/students")
        self._handle_response(resp)
        if resp.status_code == 204:
            return []
        return [ClassStudent.model_validate(s) for s in resp.json()["items"]]

    async def get_teacher_classes(self) -> list[TeacherClassItem]:
        resp = await self._client.get("/teachers/me/classes")
        self._handle_response(resp)
        if resp.status_code == 204:
            return []
        return [TeacherClassItem.model_validate(c) for c in resp.json()["items"]]

    async def get_student_classes(self) -> list[StudentClassItem]:
        resp = await self._client.get("/students/me/classes")
        self._handle_response(resp)
        if resp.status_code == 204:
            return []
        return [StudentClassItem.model_validate(c) for c in resp.json()["items"]]

    async def leave_class(self, class_id: str) -> None:
        resp = await self._client.delete(f"/students/me/classes/{class_id}")
        self._handle_response(resp)

    async def remove_student(self, class_id: str, student_id: str) -> None:
        resp = await self._client.delete(
            f"/classes/{class_id}/students/{student_id}"
        )
        self._handle_response(resp)

    # ── Quizzes ───────────────────────────────────

    async def get_teacher_quizzes(self) -> list[Quiz]:
        resp = await self._client.get("/teachers/me/quizzes")
        self._handle_response(resp)
        if resp.status_code == 204:
            return []
        return [Quiz.model_validate(q) for q in resp.json()["items"]]

    async def get_quiz(self, quiz_id: str) -> Quiz:
        resp = await self._client.get(f"/teachers/me/quizzes/{quiz_id}")
        self._handle_response(resp)
        return Quiz.model_validate(resp.json())

    # ── Submissions ───────────────────────────────

    async def submit_quiz(
        self,
        quiz_id: str,
        student_id: str,
        duration_seconds: int,
        answers: list[StudentAnswer],
    ) -> SubmitQuizResponse:
        resp = await self._client.post(
            f"/quizzes/{quiz_id}/submissions",
            json={
                "studentId": student_id,
                "durationSeconds": duration_seconds,
                "answers": [
                    {
                        "questionId": a.question_id,
                        "selectedOption": a.selected_option,
                    }
                    for a in answers
                ],
            },
        )
        self._handle_response(resp)
        return SubmitQuizResponse.model_validate(resp.json())

    # ── Recommendations ───────────────────────────

    async def get_recommendations(
        self, student_id: str
    ) -> RecommendationsResponse:
        resp = await self._client.get(
            f"/students/{student_id}/recommendations"
        )
        self._handle_response(resp)
        return RecommendationsResponse.model_validate(resp.json())

    # ── Sprint 2 grader stubs (not yet implemented in backend) ────────────

    async def get_exam(self, exam_id: str) -> Exam:
        raise NotImplementedError("Awaiting Sprint 2 backend endpoints")

    async def get_exam_image(self, exam_id: str) -> bytes:
        raise NotImplementedError("Awaiting Sprint 2 backend endpoints")

    async def submit_grading(
        self, exam_id: str, results: GradingResult
    ) -> GradingResponse:
        raise NotImplementedError("Awaiting Sprint 2 backend endpoints")

    async def update_exam_status(
        self, exam_id: str, status: ExamStatus
    ) -> None:
        raise NotImplementedError("Awaiting Sprint 2 backend endpoints")

    # ── Internal ──────────────────────────────────

    def _handle_response(self, response: httpx.Response) -> None:
        if response.is_success:
            return
        try:
            body = response.json()
            message = body.get("message", response.reason_phrase)
            error_code = body.get("errorCode", "UNKNOWN")
        except Exception:
            message = response.reason_phrase or "Unknown error"
            error_code = "UNKNOWN"
        status = response.status_code
        if status in (401, 403):
            raise AuthError(status, message, error_code)
        if status == 404:
            raise NotFoundError(status, message, error_code)
        raise ApiError(status, message, error_code)
