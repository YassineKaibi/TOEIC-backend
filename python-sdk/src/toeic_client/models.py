from enum import Enum

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    """Base model that accepts camelCase JSON and exposes snake_case attrs."""

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
    )


# ── Sprint 1 ────────────────────────────────────


class ClassStudent(CamelModel):
    id: str
    full_name: str
    email: str
    joined_at: str


# ── Sprint 2 (models will be refined when backend endpoints are designed) ──


class ExamStatus(str, Enum):
    PENDING = "pending"
    UNDER_REVIEW = "under_review"
    FINALIZED = "finalized"


class Exam(CamelModel):
    id: str
    class_id: str
    student_id: str
    status: ExamStatus
    image_url: str | None = None
    created_at: str


class QuestionResult(CamelModel):
    question_number: int
    selected_answer: str
    correct_answer: str
    is_correct: bool


class GradingResult(CamelModel):
    listening_score: int
    reading_score: int
    total_score: int
    per_question: list[QuestionResult]


class GradingResponse(CamelModel):
    exam_id: str
    status: ExamStatus
