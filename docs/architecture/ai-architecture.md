# AI Architecture

The AI Integration is designed to be provider-agnostic. We define a core interface that different LLM providers can implement.

## Abstraction
```text
AIService (Interface)
   |
   +-- OpenAIProvider (Implementation)
   +-- GeminiProvider (Implementation)
   +-- MockAIProvider (For testing)
```

## Security & Prompts
- Prompts are maintained in `docs/prompts/` and are not hardcoded into Java classes if possible.
- User code is treated as DATA. Prompts must be structured to prevent Prompt Injection.
- API keys are injected via environment variables.

## Output Validation
- All AI responses must be validated and sanitized before being stored in the database or presented to the user.
