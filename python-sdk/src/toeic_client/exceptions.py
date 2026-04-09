class ApiError(Exception):
    """Base exception for non-2xx API responses."""

    def __init__(self, status_code: int, message: str, error_code: str) -> None:
        self.status_code = status_code
        self.message = message
        self.error_code = error_code
        super().__init__(f"[{status_code}] {error_code}: {message}")


class AuthError(ApiError):
    """Raised on 401/403 responses."""


class NotFoundError(ApiError):
    """Raised on 404 responses."""
