import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterModule],
  template: `
    <aside class="sidebar flex-col justify-between">
      <div>
        <div class="logo-area flex items-center gap-sm">
          <div class="logo-icon">&lt;ai/&gt;</div>
          <span class="logo-text">AI Code Review</span>
        </div>
        
        <nav class="nav-links flex-col gap-sm">
          <a routerLink="/dashboard" routerLinkActive="active" class="nav-item">
            <span class="icon">📊</span> Dashboard
          </a>
          <a routerLink="/reviews/new" routerLinkActive="active" class="nav-item">
            <span class="icon">💻</span> Code Review
          </a>
          <a routerLink="/history" routerLinkActive="active" class="nav-item">
            <span class="icon">🕒</span> History
          </a>
          <a routerLink="/projects" routerLinkActive="active" class="nav-item">
            <span class="icon">??</span> Projects
          </a>
          <a routerLink="/settings" routerLinkActive="active" class="nav-item">
            <span class="icon">⚙️</span> Settings
          </a>
        </nav>
      </div>

      <div class="user-profile flex items-center gap-sm">
        <div class="avatar">JD</div>
        <div class="user-info flex-col">
          <span class="user-name">John Doe</span>
          <span class="user-role">Senior Developer</span>
        </div>
      </div>
    </aside>
  `,
  styles: [`
    .sidebar { width: 260px; height: 100vh; background-color: var(--bg-panel); border-right: 1px solid var(--bg-panel-border); padding: var(--spacing-lg) var(--spacing-md); flex-shrink: 0; }
    .logo-area { padding: 0 var(--spacing-sm) var(--spacing-xl); }
    .logo-icon { font-family: 'Fira Code', monospace; font-weight: 700; color: var(--accent-primary); background: rgba(88, 166, 255, 0.1); padding: 4px 8px; border-radius: 4px; }
    .logo-text { font-weight: 600; font-size: 16px; }
    .nav-item { display: flex; align-items: center; gap: 12px; padding: 10px 12px; color: var(--text-secondary); border-radius: var(--border-radius-md); transition: all 0.2s ease; font-weight: 500; font-size: 14px; }
    .nav-item:hover { background-color: var(--bg-panel-hover); color: var(--text-primary); text-decoration: none; }
    .nav-item.active { background-color: rgba(88, 166, 255, 0.1); color: var(--accent-primary); }
    .user-profile { padding: var(--spacing-md) var(--spacing-sm) 0; border-top: 1px solid var(--bg-panel-border); cursor: pointer; }
    .avatar { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, var(--accent-primary), #8b5cf6); display: flex; align-items: center; justify-content: center; color: white; font-weight: 600; font-size: 14px; }
    .user-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
    .user-role { font-size: 12px; color: var(--text-secondary); }
  `]
})
export class Sidebar {}
