# REST API Architecture

The backend exposes a REST API for the frontend.

## Planned Endpoints

### Code Reviews
- `POST /api/reviews` - Submit new code for AI review.
- `GET /api/reviews` - Get a list of all reviews (paginated).
- `GET /api/reviews/{id}` - Get detailed results of a specific review.
- `DELETE /api/reviews/{id}` - Delete a review.

### Users/Auth (Future)
- `POST /api/auth/login`
- `POST /api/auth/register`

All endpoints will accept and produce `application/json`.
