# DevelopersHub

> A developer-focused platform for discovering resources, projects, tools, and opportunities — all in one place.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-12-CC0200?logo=flyway&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![CI](https://github.com/kunalbuilds-git/DevelopersHub/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/badge/license-MIT-green)

## About

Student developers juggle hackathons, internships, GSoC-style programs, and beginner-friendly open-source issues scattered across a dozen different sites. DevelopersHub brings them into one searchable, filterable, personal place — built both as a real tool and as a hands-on backend learning project.

**v0.1 backend core is complete** — full CRUD, auth, role-based access, tagging, search, bookmarking, and CI, all hand-built and verified end-to-end.

## Features

- Full CRUD for opportunities (create, read, update, delete) with request validation and correct HTTP status codes
- Paginated listing with total count and page metadata (page size capped at 50)
- Many-to-many tagging (opportunities ↔ tags) through a normalized join table
- Combined filtering by type, tag, and keyword, built with Spring Data JPA Specifications
- Case-insensitive keyword search across title and organization, with LIKE wildcards escaped
- User registration and login with JWT authentication (BCrypt password hashing)
- Role-based access control — only admins can create, edit, or delete opportunities
- Bookmark / unbookmark opportunities, with a personal saved list sorted by deadline
- Versioned schema migrations with Flyway, plus indexes on the filtered columns
- RFC 7807 ProblemDetail error responses for validation, conflicts, and bad input
- Dockerized PostgreSQL for local development
- Continuous integration — the test suite runs on every pull request and on pushes to main

### Planned next
- Frontend (web UI)
- Deployment

## Architecture

```mermaid
flowchart LR
    Client -->|REST / JSON| Controller
    Controller --> Service
    Service --> Repository
    Service --> Spec["Specifications: type / tag / keyword"]
    Controller -->|JWT| Security["Spring Security + JwtAuthFilter"]
    Repository --> DB[(PostgreSQL)]
    Flyway["Flyway Migrations"] --> DB
```

A layered Spring Boot backend — **Controller → Service → Repository** — with dynamic filtering through composable JPA Specifications and stateless JWT-based authentication.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Data access | Spring Data JPA (Hibernate) |
| Auth | Spring Security + JJWT |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Build tool | Maven |
| Local dev | Docker Compose |
| CI | GitHub Actions |

## Local development

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Run the API (from backend/)
cd backend
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`. Configuration is environment-driven, with development defaults baked in:

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | *(generated at startup)* | HMAC key for signing tokens. **Set this in production** — at least 32 bytes. Without it a random key is generated per restart, so tokens do not survive a restart. |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) | Token lifetime in milliseconds. |
| `DB_URL` | `jdbc:postgresql://localhost:5432/developershub` | Database URL. |
| `DB_USERNAME` | `devuser` | Database user. |
| `DB_PASSWORD` | `devpass` | Database password. |
| `CORS_ALLOWED_ORIGINS` | *(empty)* | Comma-separated browser origins allowed to call the API. Empty disables CORS. |

### Tests

```bash
cd backend
./mvnw test
```

Tests run against a dedicated `developershub_test` database, which is created automatically, so local development data is never touched. The suite covers authentication, role-based access, opportunity CRUD and filtering, bookmarks, and JWT handling.

## API Overview

**Opportunities** — `/api/opportunities`

| Method | Endpoint | Auth required |
|---|---|---|
| `GET` | `/` | No — filterable by `type`, `tag`, `keyword`, paginated |
| `GET` | `/{id}` | No |
| `POST` | `/` | Admin |
| `PUT` | `/{id}` | Admin |
| `DELETE` | `/{id}` | Admin |

**Auth** — `/api/auth`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/register` | Create an account, returns a JWT |
| `POST` | `/login` | Returns a fresh JWT |

**Bookmarks** — `/api/bookmarks` (all require a logged-in user)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/{opportunityId}` | Bookmark an opportunity |
| `DELETE` | `/{opportunityId}` | Remove a bookmark |
| `GET` | `/` | My saved opportunities, sorted by deadline |

### Opportunity types

`HACKATHON`, `INTERNSHIP`, `PROGRAM`, `OPEN_SOURCE` — any other value is rejected with `400`.

### Errors

Errors are returned as RFC 7807 ProblemDetail documents:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "Validation failed",
  "errors": { "email": "must be a well-formed email address" }
}
```

| Status | When |
|---|---|
| `400` | Validation failure, malformed body, unknown opportunity type, bad parameter |
| `401` | Wrong credentials, or a token that is missing, expired, or invalid |
| `403` | Authenticated but not allowed (for example, a non-admin creating an opportunity) |
| `404` | Opportunity or bookmark does not exist |
| `409` | Email already registered, or opportunity already bookmarked |

### Example

```bash
# Register (returns a JWT)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"Password123!"}'

# Create an opportunity as an admin
curl -X POST http://localhost:8080/api/opportunities \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"title":"Spring Boot Hackathon","organization":"DevelopersHub","type":"HACKATHON","description":"Build something","url":"https://example.com","deadline":"2026-12-31T23:59:00","tags":["java","backend"]}'

# Browse and filter (no auth needed)
curl "http://localhost:8080/api/opportunities?type=HACKATHON&tag=java&keyword=spring&page=0&size=10"
```

A listing looks like this:

```json
{
  "content": [
    {
      "id": 1,
      "title": "Spring Boot Hackathon",
      "organization": "DevelopersHub",
      "type": "HACKATHON",
      "description": "Build something",
      "url": "https://example.com",
      "deadline": "2026-12-31T23:59:00",
      "createdAt": "2026-10-28T17:13:26.807",
      "tags": [{ "id": 1, "name": "java" }]
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```
