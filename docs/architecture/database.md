# Database Architecture

The application uses MySQL.

## Planned Entities
- **User:** Stores user information.
- **CodeReview:** Represents a specific code review session submitted by a user.
- **ReviewResult:** The high-level result of the review.
- **Finding:** Specific issues, bugs, or smells detected during the review.
- **ReviewHistory:** Audit logs of review activities.

## Relationships
- `User` (1) to (N) `CodeReview`
- `CodeReview` (1) to (1) `ReviewResult`
- `ReviewResult` (1) to (N) `Finding`

*Note: This is an initial draft and will evolve.*
