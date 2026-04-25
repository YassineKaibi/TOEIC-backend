# TOEIC Platform — Backend

REST API for **Thee Platform**, a Tunisian TOEIC-focused educational app.  
Teachers create classes, students join via codes, take digital quizzes, and get AI-powered recommendations.

**Stack**: Kotlin · Ktor · Exposed · PostgreSQL · JWT · Docker  
**OpenAPI spec**: [`main.yaml`](./main.yaml)

---

## Quick Start

### Prerequisites
- JDK 17+
- Docker & Docker Compose (for the full stack)

### Run locally (Docker)

```bash
cp .env.example .env   # fill in your values
docker compose up --build
```

API is available at `http://localhost:3000/api/v1`.

### Run without Docker

Start a local PostgreSQL instance, fill in `.env`, then:

```bash
./gradlew run
```

---

## Environment Variables

Copy `.env.example` to `.env` and set:

| Variable            | Description                     |
|---------------------|---------------------------------|
| `POSTGRES_HOST`     | PostgreSQL host                 |
| `POSTGRES_PORT`     | PostgreSQL port                 |
| `POSTGRES_DB`       | Database name                   |
| `POSTGRES_USER`     | Database user                   |
| `POSTGRES_PASSWORD` | Database password               |
| `JWT_SECRET`        | Secret key for signing JWTs     |
| `JWT_ISSUER`        | JWT issuer claim                |
| `JWT_AUDIENCE`      | JWT audience claim              |
| `JWT_EXPIRATION`    | Token TTL in milliseconds       |

---

## API Overview

All endpoints are prefixed with `/api/v1`. Authentication uses `Authorization: Bearer <token>`.

### Auth

| Method | Path             | Auth | Description        |
|--------|------------------|------|--------------------|
| POST   | `/auth/login`    | No   | Login, returns JWT |
| POST   | `/auth/register` | No   | Register account   |

### Classes

| Method | Path                                      | Role    | Description             |
|--------|-------------------------------------------|---------|-------------------------|
| POST   | `/classes`                                | Teacher | Create a class          |
| POST   | `/classes/join`                           | Student | Join via code           |
| GET    | `/teachers/me/classes`                    | Teacher | List teacher's classes  |
| GET    | `/students/me/classes`                    | Student | List student's classes  |
| GET    | `/classes/{classId}/students`             | Teacher | List students in class  |
| DELETE | `/classes/{classId}/students/{studentId}` | Teacher | Remove a student        |

### Quizzes & Submissions

| Method | Path                                         | Role    | Description              |
|--------|----------------------------------------------|---------|--------------------------|
| GET    | `/quizzes`                                   | Teacher | List all quizzes         |
| POST   | `/quizzes`                                   | Teacher | Create a quiz            |
| POST   | `/classes/{classId}/quizzes/{quizId}/assign` | Teacher | Assign quiz to a class   |
| POST   | `/submissions`                               | Student | Submit quiz answers      |
| GET    | `/submissions/{submissionId}`                | Student | Get submission result    |

### Recommendations

| Method | Path              | Role    | Description                        |
|--------|-------------------|---------|------------------------------------|
| GET    | `/recommendations`| Student | Get AI-powered quiz recommendations|

---

## Project Structure

```
src/main/kotlin/com/toeic/backend/
├── ai/               # AI client abstraction (recommendations)
├── auth/             # Login, registration, JWT service
├── classes/          # Class CRUD, routes, repository
├── enrollments/      # Student–class join table
├── quizzes/          # Quiz management and assignment
├── submissions/      # Quiz submission pipeline & dispatcher
├── recommendations/  # AI recommendation service
├── teachers/         # Teacher-scoped routes
├── students/         # Student-scoped routes
├── users/            # User repository & table
├── common/           # Shared error types, response helpers
├── db/               # Database factory & migrations
└── plugins/          # Ktor plugins (auth, routing, serialization, …)
```

---

## Development Commands

```bash
./gradlew build                                                              # compile & assemble
./gradlew run                                                                # run locally
./gradlew test                                                               # run all tests
./gradlew test --tests "com.toeic.backend.SomeTestClass"                    # single test class

# Docker
docker compose up --build                                                    # dev
docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build  # prod
```

---

## Error Response Shape

```json
{
  "message": "Human-readable description",
  "errorCode": "MACHINE_READABLE_CODE"
}
```

Common error codes: `INVALID_CREDENTIALS`, `TEACHER_ROLE_REQUIRED`, `ALREADY_JOINED`, `WRONG_CODE`, `NOT_CLASS_OWNER`, `CLASS_NOT_FOUND`

List endpoints return `{ "items": [...] }` or **204 No Content** when empty.
