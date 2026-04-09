import httpx

from .exceptions import ApiError, AuthError, NotFoundError
from .models import (
    ClassStudent,
    Exam,
    ExamStatus,
    GradingResponse,
    GradingResult,
)


class ToeicClient:
    """Thin client for the TOEIC Platform API."""

    def __init__(self, base_url: str, token: str, timeout: float = 30.0) -> None:
        self._client = httpx.Client(
            base_url=base_url,
            headers={"Authorization": f"Bearer {token}"},
            timeout=timeout,
        )

    def close(self) -> None:
        self._client.close()

    def __enter__(self) -> "ToeicClient":
        return self

    def __exit__(self, *args: object) -> None:
        self.close()

    # ── Sprint 1 ─────────────────────────────────

    def get_class_students(self, class_id: str) -> list[ClassStudent]:
        resp = self._client.get(f"/classes/{class_id}/students")
        self._handle_response(resp)
        if resp.status_code == 204:
            return []
        return [ClassStudent.model_validate(s) for s in resp.json()["items"]]

    # ── Sprint 2 (stubs until backend is ready) ──

    def get_exam(self, exam_id: str) -> Exam:
        raise NotImplementedError("Awaiting Sprint 2 backend endpoints")

    def get_exam_image(self, exam_id: str) -> bytes:
        raise NotImplementedError("Awaiting Sprint 2 backend endpoints")

    def submit_grading(self, exam_id: str, results: GradingResult) -> GradingResponse:
        raise NotImplementedError("Awaiting Sprint 2 backend endpoints")

    def update_exam_status(self, exam_id: str, status: ExamStatus) -> None:
        raise NotImplementedError("Awaiting Sprint 2 backend endpoints")

    # ── Internal ─────────────────────────────────

    def _handle_response(self, response: httpx.Response) -> None:
        """Raise typed exceptions for non-2xx responses."""
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
