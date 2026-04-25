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


# ── Auth ─────────────────────────────────────────


class UserInfo(CamelModel):
    id: str
    full_name: str
    email: str
    role: str  # "teacher" or "student"


class LoginResponse(CamelModel):
    access_token: str
    user: UserInfo


# ── Classes ──────────────────────────────────────


class TeacherInfo(CamelModel):
    full_name: str


class TeacherClassItem(CamelModel):
    id: str
    name: str
    students_count: int
    join_code: str


class StudentClassItem(CamelModel):
    id: str
    name: str
    teacher: TeacherInfo
    joined_at: str


# ── Quizzes ──────────────────────────────────────


class Question(CamelModel):
    id: str
    quiz_id: str
    prompt: str
    order: int
    points: float
    options: list[str]


class Quiz(CamelModel):
    id: str
    title: str
    description: str | None = None
    time_limit_minutes: int | None = None
    teacher_id: str
    created_at: str
    questions: list[Question] | None = None


# ── Submissions ──────────────────────────────────


class StudentAnswer(CamelModel):
    question_id: str
    selected_option: str


class SubmitQuizResponse(CamelModel):
    submission_id: str
    status: str  # always "pending" immediately after submission


# ── Recommendations ──────────────────────────────


class QuizRecommendation(CamelModel):
    quiz_id: str
    title: str
    difficulty: str | None = None


class RecommendationsResponse(CamelModel):
    quizzes: list[QuizRecommendation]


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
