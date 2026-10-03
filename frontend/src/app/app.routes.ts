import { Routes } from '@angular/router';
import { adminGuard, authGuard, guestGuard } from './core/guards/auth.guard';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  { path: 'login', canActivate: [guestGuard], loadComponent: () => import('./features/auth/login/login').then((m) => m.Login) },
  { path: 'register', canActivate: [guestGuard], loadComponent: () => import('./features/auth/register/register').then((m) => m.Register) },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard/dashboard').then((m) => m.Dashboard) },
      { path: 'repositories', loadComponent: () => import('./features/repositories/repositories').then((m) => m.Repositories) },
      { path: 'analyses', loadComponent: () => import('./features/analyses/analysis-list/analysis-list').then((m) => m.AnalysisList) },
      { path: 'analyses/:id', loadComponent: () => import('./features/analyses/analysis-detail/analysis-detail').then((m) => m.AnalysisDetail) },
      { path: 'architecture/:id', loadComponent: () => import('./features/architecture/architecture').then((m) => m.Architecture) },
      { path: 'rules', loadComponent: () => import('./features/rules/rules').then((m) => m.Rules) },
      { path: 'reviews/new', loadComponent: () => import('./features/code-review/code-review/code-review').then((m) => m.CodeReview) },
      { path: 'reviews/:id', loadComponent: () => import('./features/review-details/review-details/review-details').then((m) => m.ReviewDetails) },
      { path: 'history', loadComponent: () => import('./features/history/history/history').then((m) => m.History) },
      { path: 'projects', loadComponent: () => import('./features/projects/project-list/project-list').then((m) => m.ProjectList) },
      { path: 'projects/import', loadComponent: () => import('./features/projects/project-import/project-import').then((m) => m.ProjectImport) },
      { path: 'projects/:id', loadComponent: () => import('./features/projects/project-details/project-details').then((m) => m.ProjectDetails) },
      { path: 'settings', loadComponent: () => import('./features/settings/settings/settings').then((m) => m.Settings) },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
// adminGuard is exported for future admin-only routes (e.g. /admin/*)
export { adminGuard };
