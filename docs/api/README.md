# REST API (JWT required unless stated)

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register`, `/login`, `/refresh`, `/logout` (public) | authentication |
| GET | `/api/auth/me` | current user |
| GET/PUT | `/api/admin/users`, `/api/admin/users/{id}` | ADMIN only |
| GET | `/api/dashboard` | per-user counters, recent analyses, architecture overview |
| GET | `/api/github/status`, `/connect-url`, `/repositories`, `/repositories/{owner}/{repo}` | GitHub |
| GET | `/api/github/callback` (public) | OAuth callback |
| POST | `/api/analyses` `{owner, repo, branch?}` | start a repository analysis -> `202 {reviewId, analysisId, status}` |
| POST | `/api/analyses/zip/{projectId}` | deep analysis of an imported ZIP project |
| GET | `/api/analyses` | history |
| GET | `/api/analyses/{id}`, `/status`, `/findings?category&severity&file&source`, `/architecture`, `/diagrams`, `/code?path&line` | report |
| DELETE | `/api/analyses/{id}` | delete |
| GET/POST/PUT/DELETE | `/api/rules` | custom rules |
| `*` | `/api/projects/**`, `/api/reviews/**` | existing ZIP projects and single-file reviews (now authenticated, per-user) |

Error shape: `{timestamp, status, error, message, path}`.
Mapping with the requested routes: `/api/reviews` (repository analyses) = `/api/analyses` because `/api/reviews` already serves the existing single-file review feature.
