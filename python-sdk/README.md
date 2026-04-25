# toeic-client: Python SDK for TOEIC Platform

A synchronous and asynchronous Python client library for the TOEIC Platform REST API. Designed for AI services (recommendation engine, grader) to query quiz metadata and submit student results.

## Installation

### From source (development)

```bash
git clone https://github.com/YassineKaibi/TOEIC-backend.git
cd TOEIC-backend/python-sdk
pip install -e ".[dev]"  # with dev dependencies for testing
```

### From PyPI (when released)

```bash
pip install toeic-client
```

## Quick Start

### Synchronous Client

```python
from toeic_client import ToeicClient

# Initialize with teacher token
client = ToeicClient(
    base_url="http://backend:3000/api/v1",
    token="eyJ0eXAiOiJKV1QiLCJhbGc..."
)

# Fetch quizzes
quizzes = client.get_teacher_quizzes()
print(f"Found {len(quizzes)} quizzes")

# Close when done
client.close()
```

### Asynchronous Client

```python
import asyncio
from toeic_client import AsyncToeicClient

async def main():
    async with AsyncToeicClient(
        base_url="http://backend:3000/api/v1",
        token="eyJ0eXAiOiJKV1QiLCJhbGc..."
    ) as client:
        quizzes = await client.get_teacher_quizzes()
        print(f"Found {len(quizzes)} quizzes")

asyncio.run(main())
```

## Authentication

### Getting a Token

```python
from toeic_client import ToeicClient

# 1. Login to get a token
client = ToeicClient(base_url="http://backend:3000/api/v1", token="")
response = client.login(
    email="teacher@toeic.tn",
    password="secure_password"
)

token = response.access_token
print(f"Token: {token}")
print(f"User: {response.user.full_name} ({response.user.role})")

client.close()

# 2. Now use the token for subsequent requests
client = ToeicClient(
    base_url="http://backend:3000/api/v1",
    token=token
)
```

### Storing Tokens Securely

```bash
# In your .env file
TOEIC_BACKEND_URL=http://backend:3000/api/v1
TOEIC_TEACHER_TOKEN=eyJ0eXAiOiJKV1QiLCJhbGc...
```

```python
import os
from dotenv import load_dotenv
from toeic_client import ToeicClient

load_dotenv()

client = ToeicClient(
    base_url=os.getenv("TOEIC_BACKEND_URL"),
    token=os.getenv("TOEIC_TEACHER_TOKEN")
)
```

## Use Cases

### Recommendation Engine

The recommendation engine authenticates as a teacher and queries available quizzes to make personalized recommendations to students.

```python
from toeic_client import ToeicClient

client = ToeicClient(base_url=backend_url, token=teacher_token)

# 1. Get all quizzes
quizzes = client.get_teacher_quizzes()

# 2. Load quiz details (questions, difficulty)
for quiz in quizzes:
    full_quiz = client.get_quiz(quiz.id)
    print(f"Quiz: {full_quiz.title}")
    print(f"  Description: {full_quiz.description}")
    print(f"  Time Limit: {full_quiz.time_limit_minutes} minutes")
    print(f"  Questions: {len(full_quiz.questions or [])}")
    
    for q in full_quiz.questions:
        print(f"    Q{q.order}: {q.prompt}")
        print(f"      Points: {q.points}")
        print(f"      Options: {', '.join(q.options)}")

# 3. Get recommendations for a specific student
recommendations = client.get_recommendations(student_id="usr_123")

for rec in recommendations.quizzes:
    print(f"Recommend: {rec.title}")
    if rec.difficulty:
        print(f"  Difficulty: {rec.difficulty}")

client.close()
```

### Grader Service

The grader scans and grades paper TOEIC exams, then submits results via the SDK.

```python
from toeic_client import ToeicClient, StudentAnswer

client = ToeicClient(base_url=backend_url, token=teacher_token)

# Simulate grading an exam
exam_results = {
    "qq_1": "A",   # Question 1 → Answer A
    "qq_2": "C",   # Question 2 → Answer C
    "qq_3": "B",   # Question 3 → Answer B
}

answers = [
    StudentAnswer(question_id=qid, selected_option=ans)
    for qid, ans in exam_results.items()
]

# Submit graded exam
result = client.submit_quiz(
    quiz_id="qz_1",
    student_id="usr_456",
    duration_seconds=1800,  # 30 minutes
    answers=answers
)

print(f"Submission ID: {result.submission_id}")
print(f"Status: {result.status}")  # Always "pending" initially

client.close()
```

### Async Grader (High-Performance)

```python
import asyncio
from toeic_client import AsyncToeicClient, StudentAnswer

async def grade_and_submit(quiz_id, student_id, answers_dict):
    async with AsyncToeicClient(
        base_url=backend_url,
        token=teacher_token
    ) as client:
        # Fetch quiz for validation
        quiz = await client.get_quiz(quiz_id)
        print(f"Grading exam for quiz: {quiz.title}")
        
        # Prepare answers
        answers = [
            StudentAnswer(question_id=qid, selected_option=ans)
            for qid, ans in answers_dict.items()
        ]
        
        # Submit
        result = await client.submit_quiz(
            quiz_id=quiz_id,
            student_id=student_id,
            duration_seconds=1800,
            answers=answers
        )
        
        return result

# Run for multiple exams concurrently
async def grade_batch(exams):
    tasks = [
        grade_and_submit(
            quiz_id=exam["quiz_id"],
            student_id=exam["student_id"],
            answers_dict=exam["answers"]
        )
        for exam in exams
    ]
    return await asyncio.gather(*tasks)

results = asyncio.run(grade_batch([
    {"quiz_id": "qz_1", "student_id": "usr_1", "answers": {"qq_1": "A"}},
    {"quiz_id": "qz_1", "student_id": "usr_2", "answers": {"qq_1": "B"}},
]))
```

## API Reference

### Client Initialization

Both `ToeicClient` (sync) and `AsyncToeicClient` (async) share the same constructor:

```python
client = ToeicClient(
    base_url: str,           # e.g., "http://backend:3000/api/v1"
    token: str,              # JWT bearer token
    timeout: float = 30.0    # Request timeout in seconds
)
```

### Auth Methods

#### `login(email: str, password: str) -> LoginResponse`

Authenticate and receive a token.

```python
response = client.login("user@example.com", "password")
print(response.access_token)
print(response.user.id)
print(response.user.role)  # "teacher" or "student"
```

#### `register(full_name: str, email: str, password: str, role: str) -> LoginResponse`

Register a new user and receive a token.

```python
response = client.register(
    full_name="Dr. Ahmed Ben",
    email="ahmed@example.com",
    password="secure_pwd",
    role="teacher"
)
```

### Class Methods

#### `get_teacher_classes() -> list[TeacherClassItem]`

List all classes owned by the authenticated teacher.

```python
classes = client.get_teacher_classes()
for cls in classes:
    print(f"ID: {cls.id}, Name: {cls.name}, Students: {cls.students_count}")
    print(f"Join Code: {cls.join_code}")
```

#### `get_student_classes() -> list[StudentClassItem]`

List all classes the authenticated student has joined.

```python
classes = client.get_student_classes()
for cls in classes:
    print(f"Class: {cls.name}")
    print(f"Teacher: {cls.teacher.full_name}")
    print(f"Joined: {cls.joined_at}")
```

#### `leave_class(class_id: str) -> None`

Student leaves a class.

```python
client.leave_class("cls_1")
print("Left class successfully")
```

#### `remove_student(class_id: str, student_id: str) -> None`

Teacher removes a student from their class.

```python
client.remove_student(class_id="cls_1", student_id="usr_123")
print("Student removed")
```

#### `get_class_students(class_id: str) -> list[ClassStudent]`

Get roster of students in a class (teacher only).

```python
students = client.get_class_students("cls_1")
for student in students:
    print(f"{student.full_name} ({student.email})")
    print(f"  Joined: {student.joined_at}")
```

### Quiz Methods

#### `get_teacher_quizzes() -> list[Quiz]`

List all quizzes created by the authenticated teacher.

```python
quizzes = client.get_teacher_quizzes()
for quiz in quizzes:
    print(f"{quiz.title} - {len(quiz.questions or [])} questions")
```

#### `get_quiz(quiz_id: str) -> Quiz`

Fetch full quiz details including questions.

```python
quiz = client.get_quiz("qz_1")
print(f"Title: {quiz.title}")
print(f"Description: {quiz.description}")
print(f"Time Limit: {quiz.time_limit_minutes} minutes")

if quiz.questions:
    for q in quiz.questions:
        print(f"\nQ{q.order}: {q.prompt}")
        print(f"  Points: {q.points}")
        print(f"  Options: {q.options}")
```

### Submission Methods

#### `submit_quiz(quiz_id: str, student_id: str, duration_seconds: int, answers: list[StudentAnswer]) -> SubmitQuizResponse`

Submit student answers to a quiz.

```python
from toeic_client import StudentAnswer

answers = [
    StudentAnswer(question_id="qq_1", selected_option="A"),
    StudentAnswer(question_id="qq_2", selected_option="B"),
]

result = client.submit_quiz(
    quiz_id="qz_1",
    student_id="usr_456",
    duration_seconds=1800,
    answers=answers
)

print(f"Submission ID: {result.submission_id}")
print(f"Status: {result.status}")
```

### Recommendation Methods

#### `get_recommendations(student_id: str) -> RecommendationsResponse`

Get recommended quizzes for a student.

```python
response = client.get_recommendations("usr_123")

for quiz_rec in response.quizzes:
    print(f"Quiz: {quiz_rec.quiz_id}")
    print(f"Title: {quiz_rec.title}")
    print(f"Difficulty: {quiz_rec.difficulty}")
```

## Error Handling

The SDK raises three exception types:

```python
from toeic_client import AuthError, NotFoundError, ApiError

try:
    quiz = client.get_quiz("qz_999")
except NotFoundError as e:
    print(f"Quiz not found")
    print(f"  Status: {e.status_code}")
    print(f"  Message: {e.message}")
    print(f"  Error Code: {e.error_code}")
except AuthError as e:
    print(f"Authentication failed")
    # Handle token expiration, re-login, etc.
except ApiError as e:
    print(f"API error: {e.message}")
    # Handle other errors
```

### Common Error Codes

| Code | Meaning |
|------|---------|
| `INVALID_CREDENTIALS` | Login failed: wrong email/password |
| `TEACHER_ROLE_REQUIRED` | Endpoint requires teacher role |
| `CLASS_NOT_FOUND` | Class does not exist |
| `QUIZ_NOT_FOUND` | Quiz does not exist |
| `NOT_CLASS_OWNER` | Only class owner can perform this action |
| `STUDENT_MISMATCH` | Student/recommendation mismatch |

## Data Models

All models use snake_case attributes but accept/emit camelCase JSON:

```python
# All these are equivalent:
quiz.time_limit_minutes      # Python attribute
quiz.model_dump()            # {"timeLimitMinutes": 30, ...}
Quiz.model_validate({        # Accepts camelCase JSON
    "timeLimitMinutes": 30,
    ...
})
```

### Key Models

- **`LoginResponse`**: Contains `access_token` and `user` info
- **`Quiz`**: Contains `id`, `title`, `description`, `time_limit_minutes`, `questions`, etc.
- **`Question`**: Contains `id`, `prompt`, `order`, `points`, `options`
- **`StudentAnswer`**: Contains `question_id` and `selected_option`
- **`SubmitQuizResponse`**: Contains `submission_id` and `status`
- **`RecommendationsResponse`**: Contains list of `quizzes` to recommend

## Advanced Usage

### Custom Timeout

```python
# For slow networks or long-running operations
client = ToeicClient(
    base_url=backend_url,
    token=token,
    timeout=120.0  # 2 minutes
)
```

### Connection Management

```python
# Explicit close (sync)
client = ToeicClient(base_url, token)
try:
    quizzes = client.get_teacher_quizzes()
finally:
    client.close()

# Context manager (recommended)
with ToeicClient(base_url, token) as client:
    quizzes = client.get_teacher_quizzes()
# Automatically closed

# Async context manager
async with AsyncToeicClient(base_url, token) as client:
    quizzes = await client.get_teacher_quizzes()
# Automatically closed
```

### Batch Operations (Async)

```python
import asyncio
from toeic_client import AsyncToeicClient, StudentAnswer

async def submit_multiple_exams(exams):
    async with AsyncToeicClient(base_url, token) as client:
        tasks = []
        
        for exam in exams:
            task = client.submit_quiz(
                quiz_id=exam["quiz_id"],
                student_id=exam["student_id"],
                duration_seconds=exam["duration"],
                answers=[
                    StudentAnswer(qid, ans)
                    for qid, ans in exam["answers"].items()
                ]
            )
            tasks.append(task)
        
        # Wait for all submissions concurrently
        results = await asyncio.gather(*tasks)
        return results

# Usage
exams = [
    {
        "quiz_id": "qz_1",
        "student_id": "usr_1",
        "duration": 1800,
        "answers": {"qq_1": "A", "qq_2": "B"}
    },
    # ... more exams
]

results = asyncio.run(submit_multiple_exams(exams))
print(f"Submitted {len(results)} exams")
```

## Testing

Run the test suite:

```bash
cd python-sdk
pip install -e ".[dev]"
pytest -v
```

The tests use `pytest-httpx` for mocking HTTP calls, so they don't require a running backend.

### Writing Tests with the SDK

```python
import pytest
from toeic_client import ToeicClient

def test_my_feature(httpx_mock):
    # Mock the backend response
    httpx_mock.add_response(json={
        "items": [
            {"id": "qz_1", "title": "Part 5", ...}
        ]
    })
    
    # Test your code
    client = ToeicClient(base_url="http://test/api/v1", token="fake")
    quizzes = client.get_teacher_quizzes()
    
    assert len(quizzes) == 1
    assert quizzes[0].title == "Part 5"
```

## Environment Setup

### Docker Deployment

```dockerfile
FROM python:3.11-slim

WORKDIR /app

# Install SDK
COPY python-sdk /sdk
RUN pip install /sdk

# Your application code
COPY grader /app
CMD ["python", "main.py"]
```

### Docker Compose

```yaml
version: '3'
services:
  backend:
    image: toeic-backend:latest
    ports:
      - "3000:80"
    environment:
      POSTGRES_HOST: postgres
      JWT_SECRET: ${JWT_SECRET}

  grader:
    image: grader:latest
    environment:
      TOEIC_BACKEND_URL: http://backend/api/v1
      TOEIC_TEACHER_TOKEN: ${TEACHER_TOKEN}
    depends_on:
      - backend

  recommendation-engine:
    image: recommendation-engine:latest
    environment:
      TOEIC_BACKEND_URL: http://backend/api/v1
      TOEIC_TEACHER_TOKEN: ${TEACHER_TOKEN}
    depends_on:
      - backend
```

## Troubleshooting

### "Awaiting Sprint 2 backend endpoints" Error

The grader stubs (`get_exam`, `get_exam_image`, `submit_grading`, `update_exam_status`) are not yet implemented in the backend. These will be available in Sprint 2. Currently, use `submit_quiz()` for submitting results.

### Token Expiration

If you receive a 401 error:

```python
from toeic_client import AuthError

try:
    quizzes = client.get_teacher_quizzes()
except AuthError:
    # Token expired, re-authenticate
    response = client.login(email, password)
    client = ToeicClient(base_url, response.access_token)
    quizzes = client.get_teacher_quizzes()
```

### Connection Timeouts

Increase the timeout for slow networks:

```python
client = ToeicClient(
    base_url=backend_url,
    token=token,
    timeout=60.0  # 60 seconds instead of default 30
)
```

## Contributing

Contributions are welcome! To develop:

```bash
git clone https://github.com/YassineKaibi/TOEIC-backend.git
cd python-sdk
pip install -e ".[dev]"
pytest -v
```

## Support

For issues, feature requests, or questions:
- Open an issue on GitHub
- Contact the TOEIC Platform team
