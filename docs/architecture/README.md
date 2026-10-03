# Architecture

Angular SPA -> Spring Boot (single backend, no extra microservice) -> MySQL / GitHub API / LLM provider.

## System architecture
```mermaid
graph LR
  U([User]) --> FE[Angular frontend]
  FE -->|REST + JWT| BE[Spring Boot backend]
  BE --> DB[(MySQL)]
  BE -->|OAuth + REST| GH[GitHub API]
  BE -->|chat/completions| LLM[LLM provider: Gemini / OpenRouter / ...]
```

## Backend modules (package `com.aicodereview`)
| Module | Package | Role |
|---|---|---|
| Auth / User | `security`, `service.AuthService`, `controller.AuthController`, `AdminController` | register/login/refresh/logout, JWT filter, roles |
| Git | `github` | OAuth flow, encrypted token, repository listing, archive download |
| Analysis | `analysis` | scanner, parsers, technology detection, static analysis, async runner |
| Architecture | `analysis.ArchitectureAnalyzer`, `DiagramGenerator` | graph + Mermaid diagrams |
| AI | `ai`, `analysis.RepositoryAIReviewer` | LLM client + contextualised repository review |
| Rules | `rules` | custom review rules injected in the AI prompts |
| Report / history | `analysis.AnalysisService`, `dashboard` | status, findings, history, dashboard |
| Legacy (kept) | `service.Project*`, `ReviewService`, `analyzer` | ZIP import, single-file reviews, Bandit/Ruff/Gitleaks |

## Component diagram
```mermaid
graph TD
  C[Controllers] --> S[Services]
  S --> R[Repositories]
  R --> E[Entities / MySQL]
  C --> SEC[Security filter chain + JWT]
  S --> GHC[GithubApiClient]
  S --> RUN[RepositoryAnalysisRunner async]
  RUN --> SC[RepositoryScanner] --> P[Parsers] --> AA[ArchitectureAnalyzer] --> DG[DiagramGenerator]
  AA --> SA[StaticCodeAnalyzer] --> AI[RepositoryAIReviewer] --> LLM[LLMClient]
```

## Deployment diagram
```mermaid
graph LR
  B[Browser] -->|:4200| NG[ng serve / static host]
  B -->|:8081 REST| API[Spring Boot JVM 17+]
  API -->|:3306| MY[(MySQL 8 - docker-compose)]
  API -->|HTTPS| GH[api.github.com]
  API -->|HTTPS| LL[generativelanguage.googleapis.com]
```

## Database ER diagram
```mermaid
erDiagram
  users ||--o{ refresh_tokens : has
  users ||--o| github_connections : has
  users ||--o{ review_rules : owns
  users ||--o{ projects : owns
  users ||--o{ code_reviews : owns
  projects ||--o{ project_files : contains
  projects ||--o{ review_findings : "has (analysis history)"
  code_reviews ||--o{ review_findings : has
  users { bigint id PK  string username  string email  string password_hash  string role  boolean enabled }
  projects { bigint id PK  string name  string source_type  string repository_full_name  string branch  string commit_hash  string status  string stage  longtext architecture_json  longtext diagrams_json }
  review_findings { bigint id PK  string category  string severity  string file  int line_number  string source  string rule_id }
  github_connections { bigint id PK  string github_login  text encrypted_access_token }
```
Tables are created by Hibernate (`ddl-auto: update`). A *repository analysis* is a row of `projects` (`source_type` = `GITHUB` or `ZIP`) - it plays the role of the "CodeReview" history entity for whole-repository analysis.

## Repository analysis flow
```mermaid
sequenceDiagram
  participant FE as Angular
  participant API as Spring Boot
  participant GH as GitHub
  participant RUN as Async runner
  participant LLM
  FE->>API: POST /api/analyses {owner, repo, branch}
  API->>GH: GET /repos/{owner}/{repo} (token decrypted server-side)
  API-->>FE: 202 {analysisId, status: PENDING}
  API->>RUN: runGithub() @Async
  RUN->>GH: zipball of the commit (in memory, size-capped)
  RUN->>RUN: scan -> parse -> graph -> diagrams -> static findings
  RUN->>LLM: overview + riskiest files (JSON findings)
  RUN->>API: persist findings, summary, architecture, diagrams
  loop every 2 s
    FE->>API: GET /api/analyses/{id}/status
  end
  FE->>API: GET /findings, /architecture, /diagrams
```

## Static analysis vs AI analysis
| Static (deterministic, `source=STATIC`) | AI (`source=LLM`) |
|---|---|
| file filtering, technologies, imports, classes, methods | logic bugs |
| dependency graph, layering violations, cycles | security reasoning |
| cyclomatic complexity, long methods/classes | refactoring suggestions, explanations |
| secrets (masked), System.out, SQL concatenation, weak hashes | missing-test suggestions, custom rules |

The LLM never receives the whole repository: it gets (1) a compact structural overview, (2) the riskiest N files (`ANALYSIS_AI_MAX_FILES`, truncated), each with its direct relations and known static findings. Detected secrets are masked before sending; `.env`, keys and lock files are never read.
