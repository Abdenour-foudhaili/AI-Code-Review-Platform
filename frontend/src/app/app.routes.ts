import { Routes } from '@angular/router';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { 
        path: 'dashboard', 
        loadComponent: () => import('./features/dashboard/dashboard/dashboard').then(m => m.Dashboard) 
      },
      { 
        path: 'reviews/new', 
        loadComponent: () => import('./features/code-review/code-review/code-review').then(m => m.CodeReview) 
      },
      { 
        path: 'history', 
        loadComponent: () => import('./features/history/history/history').then(m => m.History) 
      },
      { 
        path: 'settings', 
        loadComponent: () => import('./features/settings/settings/settings').then(m => m.Settings) 
      }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
