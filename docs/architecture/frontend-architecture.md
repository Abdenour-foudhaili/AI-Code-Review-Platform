# Frontend Architecture

The frontend is an Angular Single Page Application (SPA).

## Structure
- `core/`: Singleton services, interceptors, guards.
- `shared/`: Reusable UI components, pipes, directives.
- `features/`: Feature modules.
  - `code-review/`: Code submission, editor, results.
  - `dashboard/`: Overview of reviews and metrics.
  - `history/`: Past reviews and search.
- `layout/`: Shell components (Navbar, Footer, Sidebar).
- `app.routes.ts`: Main application routing.
