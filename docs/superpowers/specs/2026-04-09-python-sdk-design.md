# Python SDK Abstraction Layer — Design Spec

## Overview

A thin Python API client (`toeic-client`) that sits between the ML/CV models and the TOEIC backend. It provides typed methods and Pydantic models for the subset of endpoints the models need.

## Decisions

- **Location:** `python-sdk/` directory in this repo (monorepo) — tightly coupled to `main.yaml`
- **Auth:** Token passed in by caller; SDK is auth-agnostic
- **Scope:** Only endpoints the ML/CV models consume (~5 methods)
- **Approach:** Hand-written httpx wrapper with Pydantic models (not auto-generated)

## Project Structure

```
python-sdk/
├── pyproject.toml          # Package metadata, dependencies (httpx, pydantic)
├── src/
│   └── toeic_client/
│       ├── __init__.py     # Exports ToeicClient
│       ├── client.py       # Main client class
│       ├── models.py       # Pydantic request/response models
│       └── exceptions.py   # Custom exceptions (ApiError, AuthError, etc.)
└── tests/
    ├── conftest.py
    └── test_client.py
```

- `src` layout (modern Python packaging best practice)
- Dependencies: `httpx`, `pydantic`
- Dev dependencies: `pytest`, `pytest-httpx`

## Client API Surface

```python
class ToeicClient:
    def __init__(self, base_url: str, token: str, timeout: float = 30.0): ...

    # --- Available now (Sprint 1) ---
    def get_class_students(self, class_id: str) -> list[ClassStudent]

    # --- Sprint 2 (stubs until backend is ready) ---
    def get_exam(self, exam_id: str) -> Exam
    def get_exam_image(self, exam_id: str) -> bytes
    def submit_grading(self, exam_id: str, results: GradingResult) -> GradingResponse
    def update_exam_status(self, exam_id: str, status: ExamStatus) -> None
```

- Constructor takes a pre-obtained JWT token — no login logic
- `get_class_students` is the only Sprint 1 endpoint (models need student info for grading context)
- Sprint 2 methods are defined with types now but raise `NotImplementedError` until backend endpoints exist
- All methods raise `ApiError` (or subclasses) on non-2xx responses

## Pydantic Models

```python
class ClassStudent(BaseModel):
    id: str
    full_name: str
    email: str
    joined_at: str

class Exam(BaseModel):
    id: str
    class_id: str
    student_id: str
    status: ExamStatus
    image_url: str | None
    created_at: str

class ExamStatus(str, Enum):
    PENDING = "pending"
    UNDER_REVIEW = "under_review"
    FINALIZED = "finalized"

class GradingResult(BaseModel):
    listening_score: int
    reading_score: int
    total_score: int
    per_question: list[QuestionResult]

class QuestionResult(BaseModel):
    question_number: int
    selected_answer: str      # "A", "B", "C", "D"
    correct_answer: str
    is_correct: bool

class GradingResponse(BaseModel):
    exam_id: str
    status: ExamStatus
```

Sprint 2 models (`Exam`, `GradingResult`, etc.) are best-guess based on the domain description. They will be refined when the backend endpoints are designed.

## Error Handling

```python
class ApiError(Exception):
    status_code: int
    message: str
    error_code: str           # Maps to backend error codes (CLASS_NOT_FOUND, etc.)

class AuthError(ApiError): ...      # 401/403
class NotFoundError(ApiError): ...  # 404
```

- All methods parse the backend's `{message, errorCode}` JSON shape into typed exceptions
- No retries — retry logic belongs in the calling model code
- No async — ML/CV pipelines are typically synchronous batch jobs
- Timeout: configurable via constructor, defaults to 30s
- Pydantic models use snake_case fields with `alias_generator` to map to/from the backend's camelCase JSON

## Usage Example

```python
from toeic_client import ToeicClient, GradingResult, QuestionResult

client = ToeicClient(
    base_url="http://localhost:3000/api/v1",
    token="eyJ...",
)

# Fetch exam data
exam = client.get_exam("exam_123")
image = client.get_exam_image("exam_123")

# Process with ML/CV model (outside SDK scope)
scores = model.grade(image)

# Submit results
result = client.submit_grading("exam_123", GradingResult(
    listening_score=scores.listening,
    reading_score=scores.reading,
    total_score=scores.total,
    per_question=[
        QuestionResult(question_number=1, selected_answer="B", correct_answer="B", is_correct=True),
        # ...
    ],
))

# Update status
client.update_exam_status("exam_123", ExamStatus.UNDER_REVIEW)
```
