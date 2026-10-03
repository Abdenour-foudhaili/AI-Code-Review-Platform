# AI Code Review Platform

## 1. Project Title
AI Code Review Platform

## 2. Project Description
A professional web application that helps software developers review source code using Artificial Intelligence.

## 3. Objectives
The objective is to build an extensible, scalable, and easy-to-use platform for code review, utilizing LLMs to automatically analyze code, detect bugs, find security vulnerabilities, suggest refactorings, and generate unit tests.

## 4. Main Features
- **Code Analysis:** Analyze source code using an external LLM.
- **Bug & Smell Detection:** Find bugs and maintainability issues.
- **Security Scans:** Detect potential security vulnerabilities.
- **Refactoring Suggestions:** Provide clean code recommendations.
- **Test Generation:** Generate unit tests for submitted code.
- **History & Dashboard:** Keep track of previous reviews.
- **AI Project Review MVP:** Import complete software projects via ZIP archives.
  - Automatically extracts and maps supported source files.
  - Safely ignores binaries, `node_modules`, `.git`, etc.
  - Orchestrates asynchronous AI analysis across the entire project.
  - Aggregates findings and generates a deterministic project-level quality score.
  - Interactive UI with an Original Code vs Fixed Code diff viewer.

## 5. Architecture Overview
The application uses a standard three-tier architecture:
- **Frontend:** Angular-based SPA communicating via REST.
- **Backend:** Spring Boot monolithic application.
- **Database:** MySQL for storing reviews, users, and findings.
- **AI Integration:** An abstracted AI service layer integrating with external LLMs (e.g., OpenAI, Gemini).

See `docs/architecture/` for detailed architectural documentation.

## 6. Technology Stack
- **Frontend:** Angular, TypeScript, HTML5, CSS
- **Backend:** Java, Spring Boot, Spring Web, Spring Data JPA, Hibernate, Lombok
- **Database:** MySQL
- **AI Integration:** REST API / JSON communication to external LLM providers

## 7. Repository Structure
```text
AI-Code-Review-Platform/
├── frontend/       # Angular frontend application
├── backend/        # Spring Boot backend application
├── ai-service/     # AI integration stubs or modules
├── docs/           # Documentation and diagrams
├── .gitignore
├── docker-compose.yml
└── README.md
```

## 8. Development Setup
### Prerequisites
- JDK 17
- Node.js & npm
- Docker (for MySQL)

### Backend
1. Navigate to `backend/`
2. Run `./mvnw spring-boot:run`
(The server will start on port 8080)

### Frontend
1. Navigate to `frontend/`
2. Run `npm install`
3. Run `npm start`
(The app will be accessible at http://localhost:4200)

## 9. Environment Variables
Do **NOT** commit secrets or API keys. Create a `.env` file (which is gitignored) with:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `LLM_API_KEY`

## 10. Team Collaboration Instructions
We use the Feature Branch Workflow:

1. **Clone the repository:**
   `git clone <repo-url>`
2. **Switch to main branch and pull latest changes:**
   `git checkout main`
   `git pull origin main`
3. **Create a new branch for your feature:**
   `git checkout -b feature/your-feature-name`
   *(Also use `fix/` for bugs, `docs/` for documentation)*
4. **Make changes, add, and commit:**
   `git add .`
   `git commit -m "feat: describe what you did"`
5. **Push your branch:**
   `git push origin feature/your-feature-name`
6. **Open a Pull Request (PR):**
   Merge into `develop` or `main` after review.

## 11. Future Features
- Support for Docker & CI/CD
- Advanced Dashboard with Analytics
- Multiple LLM Providers

## Quick start (authentication + GitHub + repository analysis)

1. `docker compose up -d db` (MySQL) - or use your own MySQL.
2. Copy `.env.example` to `.env`, fill at least `JWT_SECRET`, `GITHUB_CLIENT_ID/SECRET`, `LLM_PROVIDER` and `LLM_API_KEY` and export them in your shell/IDE run configuration (the backend reads environment variables).
3. Backend: `cd backend && ./mvnw clean test && ./mvnw spring-boot:run` (port 8081).
4. Frontend: `cd frontend && npm install && npm test && npm start` (port 4200).
5. Open http://localhost:4200, register, connect GitHub, pick a repository, click *Analyze Repository*.

Existing `GET/POST` APIs now require a JWT. See `docs/` (architecture, auth, github, ai, api).
If your MySQL schema was created by an older version, the new columns are added by Hibernate (`ddl-auto: update`); if enum errors appear on `review_findings.category`, run `ALTER TABLE review_findings MODIFY category varchar(30);`.

If you already had a `users` table from an earlier prototype and registration fails with a SQL error about a missing default value, inspect it with `DESCRIBE users;`. If it holds no data you need, run `DROP TABLE IF EXISTS refresh_tokens, github_connections, review_rules, users;` and restart the backend: Hibernate recreates the tables.
