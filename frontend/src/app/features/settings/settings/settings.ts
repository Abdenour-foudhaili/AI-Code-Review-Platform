import { Component } from '@angular/core';

@Component({
  selector: 'app-settings',
  standalone: true,
  template: `
    <div class="settings-page flex-col gap-lg">
      <header class="page-header">
        <h1>Settings</h1>
        <p class="text-secondary">Manage your preferences and AI configuration.</p>
      </header>

      <div class="settings-grid grid grid-cols-2 gap-lg">
        <section class="settings-panel flex-col gap-md">
          <h2>Profile</h2>
          <div class="form-group">
            <label>Display Name</label>
            <input type="text" value="John Doe" />
          </div>
          <div class="form-group">
            <label>Email Address</label>
            <input type="email" value="john.doe@example.com" />
          </div>
          <div class="form-group">
            <label>Role</label>
            <input type="text" value="Senior Developer" disabled />
          </div>
        </section>

        <section class="settings-panel flex-col gap-md">
          <h2>AI Preferences</h2>
          <div class="form-group">
            <label>Default Programming Language</label>
            <select>
              <option>Auto-detect</option>
              <option selected>Java</option>
              <option>TypeScript</option>
              <option>Python</option>
            </select>
          </div>
          <div class="form-group">
            <label>AI Response Detail Level</label>
            <select>
              <option>Concise</option>
              <option selected>Standard</option>
              <option>Detailed</option>
            </select>
          </div>
          <div class="form-group checkbox-group">
            <label class="flex items-center gap-sm">
              <input type="checkbox" checked /> Always check for Security Vulnerabilities
            </label>
          </div>
        </section>

        <section class="settings-panel flex-col gap-md">
          <h2>Appearance</h2>
          <div class="form-group">
            <label>Theme</label>
            <select>
              <option>System Default</option>
              <option>Light</option>
              <option selected>Dark</option>
            </select>
          </div>
        </section>
      </div>
      
      <div class="actions flex justify-end gap-sm mt-md">
        <button class="btn btn-outline">Cancel</button>
        <button class="btn btn-primary">Save Changes</button>
      </div>
    </div>
  `,
  styles: [`
    .settings-panel {
      background-color: var(--bg-panel);
      border: 1px solid var(--bg-panel-border);
      border-radius: var(--border-radius-lg);
      padding: var(--spacing-lg);
    }
    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .form-group label {
      font-size: 14px;
      font-weight: 500;
      color: var(--text-primary);
    }
    .checkbox-group label {
      font-weight: 400;
      cursor: pointer;
    }
    input[disabled] {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .btn {
      padding: 8px 16px; border-radius: var(--border-radius-md); font-size: 14px; font-weight: 600;
      cursor: pointer; border: none; transition: all 0.2s;
    }
    .btn-outline { border: 1px solid var(--bg-panel-border); color: var(--text-primary); background: transparent; }
    .btn-primary { background-color: var(--accent-primary); color: white; }
    .mt-md { margin-top: var(--spacing-md); }
  `]
})
export class Settings {}
