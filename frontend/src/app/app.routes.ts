import { Routes } from '@angular/router';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard/dashboard').then(m => m.Dashboard) },
      { path: 'reviews/new', loadComponent: () => import('./features/code-review/code-review/code-review').then(m => m.CodeReview) },
      { path: 'reviews/:id', loadComponent: () => import('./features/review-details/review-details/review-details').then(m => m.ReviewDetails) },
      { path: 'history', loadComponent: () => import('./features/history/history/history').then(m => m.History) },
      { path: 'projects', loadComponent: () => import('./features/projects/project-list/project-list').then(m => m.ProjectList) },
      { path: 'projects/import', loadComponent: () => import('./features/projects/project-import/project-import').then(m => m.ProjectImport) },
      { path: 'projects/:id', loadComponent: () => import('./features/projects/project-details/project-details').then(m => m.ProjectDetails) },
      { path: 'settings', loadComponent: () => import('./features/settings/settings/settings').then(m => m.Settings) }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
