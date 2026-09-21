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
