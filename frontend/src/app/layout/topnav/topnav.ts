import { Component } from '@angular/core';

@Component({
  selector: 'app-topnav',
  standalone: true,
  template: `
    <header class="topnav flex items-center justify-between">
      <div class="search-container flex items-center">
        <span class="search-icon">🔍</span>
        <input type="text" placeholder="Search reviews, projects, or issues..." class="search-input" />
      </div>
      <div class="actions flex items-center gap-md">
        <button class="icon-btn" aria-label="Notifications">🔔</button>
      </div>
    </header>
  `,
  styles: [`
    .topnav {
      height: 64px;
      border-bottom: 1px solid var(--bg-panel-border);
      background-color: var(--bg-main);
      padding: 0 var(--spacing-lg);
    }
    .search-container {
      position: relative;
      width: 400px;
    }
    .search-icon {
      position: absolute;
      left: 12px;
      color: var(--text-muted);
      font-size: 14px;
    }
    .search-input {
      width: 100%;
      padding-left: 36px;
      background-color: var(--bg-panel);
      border-color: var(--bg-panel-border);
      border-radius: 20px;
    }
    .icon-btn {
      background: none;
      border: none;
      color: var(--text-secondary);
      cursor: pointer;
      font-size: 18px;
      padding: 8px;
      border-radius: 50%;
      transition: background-color 0.2s;
    }
    .icon-btn:hover {
      background-color: var(--bg-panel-hover);
      color: var(--text-primary);
    }
  `]
})
export class Topnav {}
