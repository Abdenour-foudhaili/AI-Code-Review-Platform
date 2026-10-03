# AI integration

Provider selected with `LLM_PROVIDER` (all use an OpenAI-compatible endpoint):

| Provider | Default model | Notes |
|---|---|---|
| `gemini` (recommended) | `gemini-2.5-flash` | free API key (Google AI Studio), 1M-token context window, ~10 requests/min on the free tier |
| `openrouter` | `openrouter/free` | free models, variable quotas |
| `mistral`, `groq` | `mistral-small-latest`, `llama-3.3-70b-versatile` | free tiers |
| `mock` | - | no AI call (static analysis only) |

`LLM_MIN_INTERVAL_MS` (default 7000) throttles calls; HTTP 429 triggers a retry with `Retry-After`, then the AI step stops and the report is still produced with the static findings and a warning. Invalid JSON from the model is tolerated (fences/prose stripped, malformed findings skipped).
Free quotas change often: check the provider's current limits.
