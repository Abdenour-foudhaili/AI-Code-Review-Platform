# GitHub OAuth

1. Create an OAuth App on GitHub (Settings > Developer settings). Callback URL: `http://localhost:8081/api/github/callback`.
2. Put `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GITHUB_REDIRECT_URI` in the environment.
3. Flow: Angular `GET /api/github/connect-url` (JWT) -> browser goes to GitHub -> GitHub redirects to `/api/github/callback?code&state` -> the backend validates `state` (signed, 10 min, bound to the user), exchanges the code, stores the token **AES-256-GCM encrypted**, redirects to `/repositories?github=connected`.
4. The token is never returned by any endpoint, never logged and never sent to the browser.

Endpoints: `GET /api/github/status`, `/connect-url`, `/repositories`, `/repositories/{owner}/{repo}`, `DELETE /api/github/connection`.
Requested scope: `repo read:user` (needed for private repositories; use `public_repo` through `GITHUB_SCOPE` to restrict).
