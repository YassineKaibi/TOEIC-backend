# SDK Documentation Quick Reference

This directory contains comprehensive documentation for AI developers working with the TOEIC Platform Python SDK.

## Documentation Files

### [README.md](README.md) — Complete API Reference
**For:** All developers  
**Contains:**
- Installation instructions
- Quick start examples
- Authentication guide
- Full API reference for all methods
- Data models
- Error handling
- Testing guide
- Deployment examples

**Best for:** Looking up specific API methods or troubleshooting.

### [ONBOARDING.md](ONBOARDING.md) — Step-by-Step Developer Guide
**For:** AI developers (recommendation engine, grader)  
**Contains:**
- Prerequisites and setup
- Authentication workflow
- Recommendation engine implementation (complete code example)
- Grader service implementation (complete code example)
- Docker deployment setup
- FAQ and troubleshooting

**Best for:** Getting started quickly and understanding the full workflow.

---

## Getting Started (5 minutes)

### 1. Install
```bash
cd python-sdk
pip install -e ".[dev]"
```

### 2. Authenticate
```bash
python -c "
from toeic_client import ToeicClient
client = ToeicClient('http://localhost:3000/api/v1', '')
response = client.login('your-email@example.com', 'password')
print(f'Token: {response.access_token}')
"
```

### 3. Test
```bash
pytest -v
```

---

## Common Tasks

### I want to...

| Task | File | Section |
|------|------|---------|
| **Install the SDK** | README.md | Installation |
| **Get my token** | ONBOARDING.md | Authentication Setup |
| **Build recommendation engine** | ONBOARDING.md | Recommendation Engine |
| **Build grader service** | ONBOARDING.md | Grader Service |
| **Look up API methods** | README.md | API Reference |
| **Deploy with Docker** | ONBOARDING.md | Deployment |
| **Handle errors** | README.md | Error Handling |
| **Write tests** | README.md | Testing |
| **Understand implementation details** | SPEC.md | (section-by-section) |

---

##  Key Features

-  **Sync & Async clients** — Choose based on your needs
-  **Full type hints** — Works with mypy and IDE autocompletion
-  **Comprehensive error handling** — AuthError, NotFoundError, ApiError
-  **Test-friendly** — Built-in pytest-httpx mocking support
-  **Production-ready** — Connection pooling, timeouts, retries

---

##  Support

- **Questions?** → Read ONBOARDING.md first
- **API questions?** → Check README.md's API Reference
- **Bug?** → Open a GitHub issue
- **Feature request?** → Discuss with the team

---

##  Quick Test

Verify the SDK is working:

```bash
python -c "
from toeic_client import (
    ToeicClient, AsyncToeicClient,
    LoginResponse, Quiz, StudentAnswer
)
print('✓ All imports successful')
print('✓ SDK ready to use')
"
```

---

##  Project Structure

```
python-sdk/
├── README.md              ← Full documentation
├── ONBOARDING.md          ← Developer onboarding guide
├── SPEC.md                ← Technical specification
├── pyproject.toml         ← Dependencies
├── src/
│   └── toeic_client/
│       ├── __init__.py    ← Public API exports
│       ├── client.py      ← Sync client
│       ├── async_client.py ← Async client
│       ├── models.py      ← Data models
│       └── exceptions.py  ← Error types
└── tests/
    ├── conftest.py        ← Pytest fixtures
    ├── test_client.py     ← Sync client tests
    └── test_async_client.py ← Async client tests
```

---

##  Environment Setup

Create a `.env` file in your project:

```bash
TOEIC_BACKEND_URL=http://localhost:3000/api/v1
TOEIC_TEACHER_TOKEN=your-token-here
```

Then load it in your code:

```python
from dotenv import load_dotenv
import os

load_dotenv()

backend_url = os.getenv("TOEIC_BACKEND_URL")
token = os.getenv("TOEIC_TEACHER_TOKEN")
```

---

**Last Updated:** April 25, 2026  
**SDK Version:** 0.1.0  
**Python:** 3.11+
