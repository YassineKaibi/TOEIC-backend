import httpx
import pytest

from toeic_client import (
    ApiError,
    AuthError,
    ClassStudent,
    NotFoundError,
    ToeicClient,
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
