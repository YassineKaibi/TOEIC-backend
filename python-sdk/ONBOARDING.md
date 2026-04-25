# AI Developer Onboarding Guide

Welcome to the TOEIC Platform SDK! This guide is specifically for AI developers building the recommendation engine and grader services.

## Table of Contents

1. [Before You Start](#before-you-start)
2. [Installation](#installation)
3. [Authentication Setup](#authentication-setup)
4. [Recommendation Engine](#recommendation-engine)
5. [Grader Service](#grader-service)
6. [Deployment](#deployment)
7. [FAQ](#faq)

---

## Before You Start

### What You'll Be Building

The TOEIC Platform has two AI services that use this SDK:

1. **Recommendation Engine** — A Python service that:
   - Listens for student quiz result events
   - Analyzes student performance
   - Recommends relevant quizzes for improvement
   - Queries quiz metadata via the SDK

2. **Grader** — A Python service that:
   - Receives scanned paper TOEIC exam images
   - Uses AI/ML to grade answers (listening & reading)
   - Submits graded results back to the platform via the SDK

### Prerequisites

- Python 3.11+
- Access to the TOEIC backend service URL
- A teacher account for authentication (credentials provided by admin)
- Docker (optional, but recommended for deployment)

---

## Installation

### Step 1: Clone the Repository

```bash
git clone https://github.com/YassineKaibi/TOEIC-backend.git
cd TOEIC-backend/python-sdk
```

### Step 2: Install the SDK

```bash
# For development
pip install -e ".[dev]"

# Or just the SDK without dev tools
pip install -e .
```

### Step 3: Verify Installation

```python
from toeic_client import ToeicClient, AsyncToeicClient
print("✓ SDK installed successfully")
```

---

## Authentication Setup

### Getting Your Teacher Token

The SDK requires a **teacher token** for authentication. Here's how to get one:

```python
from toeic_client import ToeicClient

# Create a temporary client (token can be empty initially)
client = ToeicClient(
    base_url="http://localhost:3000/api/v1",  # or your backend URL
    token=""  # Will fail for protected endpoints, but works for login
)

# Login with your teacher credentials
response = client.login(
    email="your-email@example.com",
    password="your-password"
)

print(f"Token: {response.access_token}")
print(f"User: {response.user.full_name}")
print(f"Role: {response.user.role}")

client.close()
```

### Storing Your Token Securely

**Never hardcode tokens in your code.** Use environment variables or a secure vault:

**`.env` file (in your project root):**

```
TOEIC_BACKEND_URL=http://backend:3000/api/v1
TOEIC_TEACHER_TOKEN=eyJ0eXAiOiJKV1QiLCJhbGc...
```

**Load in your code:**

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

**In Docker/Kubernetes, use secrets:**

```yaml
environment:
  TOEIC_BACKEND_URL: http://backend:8080/api/v1
  TOEIC_TEACHER_TOKEN: ${TEACHER_TOKEN}  # from secret
```

---

## Recommendation Engine

### Architecture

The recommendation engine is an async service that:

1. **Subscribes** to student quiz result events (via webhook, message queue, or polling)
2. **Analyzes** performance data to identify weak areas
3. **Queries** available quizzes via the SDK
4. **Recommends** the most relevant quizzes to each student

### Implementation Example

```python
import asyncio
import os
from dotenv import load_dotenv
from toeic_client import AsyncToeicClient

load_dotenv()

class RecommendationEngine:
    def __init__(self):
        self.client = None
        self.quizzes_cache = {}
    
    async def start(self):
        """Initialize the recommendation engine."""
        self.client = AsyncToeicClient(
            base_url=os.getenv("TOEIC_BACKEND_URL"),
            token=os.getenv("TOEIC_TEACHER_TOKEN")
        )
        print("✓ Connected to backend")
        
        # Pre-load quizzes on startup
        await self.load_quizzes()
    
    async def load_quizzes(self):
        """Cache all available quizzes."""
        try:
            quizzes = await self.client.get_teacher_quizzes()
            for quiz in quizzes:
                self.quizzes_cache[quiz.id] = quiz
            print(f"✓ Loaded {len(self.quizzes_cache)} quizzes")
        except Exception as e:
            print(f"✗ Failed to load quizzes: {e}")
    
    async def get_quiz_details(self, quiz_id: str):
        """Fetch full quiz details including questions."""
        try:
            quiz = await self.client.get_quiz(quiz_id)
            return {
                "id": quiz.id,
                "title": quiz.title,
                "description": quiz.description,
                "time_limit": quiz.time_limit_minutes,
                "num_questions": len(quiz.questions or []),
                "total_points": sum(q.points for q in (quiz.questions or [])),
            }
        except Exception as e:
            print(f"✗ Failed to get quiz {quiz_id}: {e}")
            return None
    
    async def analyze_student_performance(self, student_id: str, results: dict) -> list:
        """
        Analyze student performance and return recommended quiz IDs.
        
        Args:
            student_id: The student's ID
            results: Dict of performance metrics {"listening_score": 75, "reading_score": 65}
        
        Returns:
            List of recommended quiz IDs (sorted by relevance)
        """
        recommendations = []
        
        # Simple heuristic: recommend quizzes in weak areas
        if results.get("listening_score", 100) < 80:
            # Recommend listening comprehension quizzes
            for quiz_id, quiz in self.quizzes_cache.items():
                if "listening" in quiz.title.lower() or "part 3" in quiz.title.lower():
                    recommendations.append(quiz_id)
        
        if results.get("reading_score", 100) < 80:
            # Recommend reading comprehension quizzes
            for quiz_id, quiz in self.quizzes_cache.items():
                if "reading" in quiz.title.lower() or "part 5" in quiz.title.lower():
                    recommendations.append(quiz_id)
        
        # Get full details for recommendations
        detailed_recs = []
        for quiz_id in recommendations[:5]:  # Top 5 recommendations
            details = await self.get_quiz_details(quiz_id)
            if details:
                detailed_recs.append(details)
        
        return detailed_recs
    
    async def process_quiz_result(self, student_id: str, quiz_result: dict):
        """Process a student's quiz result and generate recommendations."""
        print(f"\nProcessing quiz result for student {student_id}")
        
        # Extract performance metrics
        performance = {
            "listening_score": quiz_result.get("listening_score", 0),
            "reading_score": quiz_result.get("reading_score", 0),
        }
        
        # Get recommendations
        recommendations = await self.analyze_student_performance(student_id, performance)
        
        if recommendations:
            print(f"✓ Generated {len(recommendations)} recommendations:")
            for rec in recommendations:
                print(f"  - {rec['title']} ({rec['num_questions']} questions, {rec['total_points']} points)")
        else:
            print("ℹ No recommendations at this time")
        
        return recommendations
    
    async def stop(self):
        """Shutdown the engine."""
        if self.client:
            await self.client.aclose()
        print("✓ Engine stopped")


async def main():
    engine = RecommendationEngine()
    await engine.start()
    
    # Simulate receiving a quiz result
    quiz_result = {
        "student_id": "usr_123",
        "quiz_id": "qz_1",
        "listening_score": 65,
        "reading_score": 72,
    }
    
    await engine.process_quiz_result(
        quiz_result["student_id"],
        quiz_result
    )
    
    await engine.stop()


if __name__ == "__main__":
    asyncio.run(main())
```

### Running the Recommendation Engine

```bash
# Install dependencies
pip install python-dotenv fastapi uvicorn

# Run
python recommendation_engine.py
```

### Integrating with Your Service

```python
# If using FastAPI
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI()
engine = RecommendationEngine()

@app.on_event("startup")
async def startup():
    await engine.start()

@app.on_event("shutdown")
async def shutdown():
    await engine.stop()

class QuizResult(BaseModel):
    student_id: str
    quiz_id: str
    listening_score: int
    reading_score: int

@app.post("/recommendations")
async def get_recommendations(result: QuizResult):
    try:
        recs = await engine.process_quiz_result(
            result.student_id,
            result.dict()
        )
        return {"recommendations": recs}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
```

---

## Grader Service

### Architecture

The grader service is an async service that:

1. **Receives** scanned exam images (from S3, API endpoint, message queue, etc.)
2. **Processes** the images using OCR and ML to extract answers
3. **Submits** graded results back to the platform via the SDK
4. **Tracks** grading status (pending → under review → finalized)

### Implementation Example

```python
import asyncio
import os
from dotenv import load_dotenv
from toeic_client import AsyncToeicClient, StudentAnswer

load_dotenv()

class ExamGrader:
    def __init__(self):
        self.client = None
    
    async def start(self):
        """Initialize the grader."""
        self.client = AsyncToeicClient(
            base_url=os.getenv("TOEIC_BACKEND_URL"),
            token=os.getenv("TOEIC_TEACHER_TOKEN")
        )
        print("✓ Grader initialized")
    
    async def extract_answers(self, image_path: str) -> dict:
        """
        Extract answers from a scanned exam image.
        
        This is a placeholder. In production, use:
        - Tesseract/EasyOCR for text extraction
        - OpenCV for image processing
        - ML model for answer detection
        
        Returns:
            Dict mapping question IDs to selected answers
            {"qq_1": "A", "qq_2": "B", ...}
        """
        print(f"  Processing image: {image_path}")
        
        # TODO: Implement actual OCR/ML logic
        # For now, return dummy data
        return {
            "qq_1": "A",
            "qq_2": "C",
            "qq_3": "B",
            "qq_4": "D",
        }
    
    async def grade_exam(
        self,
        exam_id: str,
        student_id: str,
        quiz_id: str,
        image_path: str,
        duration_seconds: int = 1800
    ) -> dict:
        """
        Grade a scanned exam and submit results.
        
        Args:
            exam_id: Unique exam identifier
            student_id: Student's user ID
            quiz_id: Quiz ID that was taken
            image_path: Path to scanned exam image
            duration_seconds: How long student spent on exam
        
        Returns:
            Submission response with submission_id and status
        """
        try:
            print(f"\n{'='*50}")
            print(f"Grading Exam {exam_id}")
            print(f"{'='*50}")
            
            # Step 1: Extract answers from image
            print("Step 1: Extracting answers from image...")
            answers_dict = await self.extract_answers(image_path)
            print(f"  ✓ Extracted {len(answers_dict)} answers")
            
            # Step 2: Fetch quiz to validate (optional)
            print("Step 2: Fetching quiz details...")
            try:
                quiz = await self.client.get_quiz(quiz_id)
                print(f"  ✓ Quiz: {quiz.title} ({len(quiz.questions or [])} questions)")
            except Exception as e:
                print(f"  ⚠ Could not fetch quiz: {e}")
            
            # Step 3: Convert answers to SDK format
            print("Step 3: Formatting answers...")
            answers = [
                StudentAnswer(question_id=qid, selected_option=ans)
                for qid, ans in answers_dict.items()
            ]
            print(f"  ✓ Formatted {len(answers)} answers")
            
            # Step 4: Submit to backend
            print("Step 4: Submitting to backend...")
            result = await self.client.submit_quiz(
                quiz_id=quiz_id,
                student_id=student_id,
                duration_seconds=duration_seconds,
                answers=answers
            )
            
            print(f"  ✓ Submission ID: {result.submission_id}")
            print(f"  ✓ Status: {result.status}")
            
            return {
                "exam_id": exam_id,
                "submission_id": result.submission_id,
                "status": result.status,
                "answers_count": len(answers),
            }
        
        except Exception as e:
            print(f"  ✗ Error grading exam: {e}")
            raise
    
    async def grade_batch(self, exams: list) -> list:
        """
        Grade multiple exams concurrently.
        
        Args:
            exams: List of exam dicts with keys:
                   exam_id, student_id, quiz_id, image_path, duration_seconds
        
        Returns:
            List of submission results
        """
        print(f"\nGrading batch of {len(exams)} exams...")
        
        tasks = [
            self.grade_exam(
                exam_id=e["exam_id"],
                student_id=e["student_id"],
                quiz_id=e["quiz_id"],
                image_path=e["image_path"],
                duration_seconds=e.get("duration_seconds", 1800)
            )
            for e in exams
        ]
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        # Count successes and failures
        successes = [r for r in results if not isinstance(r, Exception)]
        failures = [r for r in results if isinstance(r, Exception)]
        
        print(f"\n{'='*50}")
        print(f"Batch Complete: {len(successes)} succeeded, {len(failures)} failed")
        print(f"{'='*50}")
        
        return successes
    
    async def stop(self):
        """Shutdown the grader."""
        if self.client:
            await self.client.aclose()
        print("✓ Grader stopped")


async def main():
    grader = ExamGrader()
    await grader.start()
    
    # Grade a single exam
    result = await grader.grade_exam(
        exam_id="exam_001",
        student_id="usr_123",
        quiz_id="qz_1",
        image_path="./scans/exam_001.png",
        duration_seconds=1800
    )
    
    print(f"\nResult: {result}")
    
    await grader.stop()


if __name__ == "__main__":
    asyncio.run(main())
```

### Grading Workflow

```python
# If using a message queue (e.g., RabbitMQ, SQS)

import asyncio
from grader import ExamGrader

async def process_grading_queue():
    """Process exams from a message queue."""
    grader = ExamGrader()
    await grader.start()
    
    while True:
        # Get next exam from queue
        exam = await get_exam_from_queue()  # your queue logic
        
        if not exam:
            await asyncio.sleep(1)
            continue
        
        try:
            result = await grader.grade_exam(
                exam_id=exam["id"],
                student_id=exam["student_id"],
                quiz_id=exam["quiz_id"],
                image_path=exam["image_path"]
            )
            
            # Mark as processed in queue
            await acknowledge_exam(exam["id"])
            
        except Exception as e:
            # Log error and retry
            print(f"Failed to grade {exam['id']}: {e}")
            await requeue_exam(exam["id"], retry_count=exam.get("retries", 0) + 1)
    
    await grader.stop()


# Run in your async service
asyncio.run(process_grading_queue())
```

---

## Deployment

### Docker Setup

**Dockerfile for Grader:**

```dockerfile
FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    libtesseract-dev \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy grader code
COPY grader .

# Run grader
CMD ["python", "main.py"]
```

**Dockerfile for Recommendation Engine:**

```dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY recommendation_engine .

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

**requirements.txt (for both):**

```
toeic-client
python-dotenv
asyncio
aiohttp
# Add your additional dependencies
```

### Docker Compose

```yaml
version: '3.8'

services:
  backend:
    image: toeic-backend:latest
    ports:
      - "3000:80"
    environment:
      POSTGRES_HOST: postgres
      POSTGRES_DB: toeic_backend
      JWT_SECRET: ${JWT_SECRET}
      JWT_ISSUER: toeic-backend
      JWT_AUDIENCE: toeic-app

  grader:
    build: ./grader
    environment:
      TOEIC_BACKEND_URL: http://backend/api/v1
      TOEIC_TEACHER_TOKEN: ${TEACHER_TOKEN}
      LOG_LEVEL: INFO
    depends_on:
      - backend
    volumes:
      - ./grader/scans:/app/scans  # Mounted exam images

  recommendation-engine:
    build: ./recommendation_engine
    ports:
      - "8000:8000"
    environment:
      TOEIC_BACKEND_URL: http://backend/api/v1
      TOEIC_TEACHER_TOKEN: ${TEACHER_TOKEN}
      LOG_LEVEL: INFO
    depends_on:
      - backend
```

**Start services:**

```bash
export TEACHER_TOKEN="your-token-here"
docker compose up --build
```

---

## FAQ

### Q: How do I rotate the teacher token?

A: Tokens expire after a set period (check backend config). To refresh:

```python
from toeic_client import AuthError

async def refresh_token():
    try:
        quizzes = await client.get_teacher_quizzes()
    except AuthError:
        # Token expired, get a new one
        response = await client.login(email, password)
        new_token = response.access_token
        
        # Update environment or configuration
        os.environ["TOEIC_TEACHER_TOKEN"] = new_token
        
        # Reconnect
        client = AsyncToeicClient(base_url, new_token)
```

### Q: What if the backend is temporarily unavailable?

A: Implement retry logic:

```python
import asyncio
from toeic_client import ApiError

async def fetch_with_retry(func, max_retries=3, backoff_secs=2):
    for attempt in range(max_retries):
        try:
            return await func()
        except ApiError as e:
            if attempt == max_retries - 1:
                raise
            print(f"Attempt {attempt+1} failed. Retrying in {backoff_secs}s...")
            await asyncio.sleep(backoff_secs)
            backoff_secs *= 2

# Usage
quizzes = await fetch_with_retry(client.get_teacher_quizzes)
```

### Q: Can I use the sync client instead of async?

A: Yes! Use `ToeicClient` instead of `AsyncToeicClient`:

```python
from toeic_client import ToeicClient

client = ToeicClient(base_url, token)
quizzes = client.get_teacher_quizzes()
client.close()
```

However, **async is recommended** for production services because it's more efficient for I/O-bound operations.

### Q: How do I handle large batches of exams?

A: Use async batch processing with semaphores to limit concurrency:

```python
import asyncio
from toeic_client import AsyncToeicClient

async def grade_with_limit(exams, max_concurrent=5):
    semaphore = asyncio.Semaphore(max_concurrent)
    
    async def grade_one(exam):
        async with semaphore:
            return await grader.grade_exam(
                exam_id=exam["id"],
                student_id=exam["student_id"],
                quiz_id=exam["quiz_id"],
                image_path=exam["image_path"]
            )
    
    tasks = [grade_one(exam) for exam in exams]
    return await asyncio.gather(*tasks)

results = await grade_with_limit(exams, max_concurrent=10)
```

### Q: What error codes should I handle?

A: Watch for these common codes:

| Code | Action |
|------|--------|
| `INVALID_CREDENTIALS` | Invalid token → re-login |
| `QUIZ_NOT_FOUND` | Quiz doesn't exist → check ID |
| `TEACHER_ROLE_REQUIRED` | Use teacher account → check token |
| `STUDENT_MISMATCH` | Student ID mismatch → validate input |

### Q: Where do I report bugs or request features?

A: Open an issue on GitHub: https://github.com/YassineKaibi/TOEIC-backend/issues

---

## Next Steps

1.  Install the SDK
2.  Get your teacher token
3.  Test with a simple script
4.  Build your recommendation engine or grader
5.  Deploy with Docker
6.  Monitor and iterate

**Need help?** Check the main [README.md](README.md) for more detailed API docs, or contact the platform team.
