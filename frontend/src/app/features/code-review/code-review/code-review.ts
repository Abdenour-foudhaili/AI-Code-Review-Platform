import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-code-review',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="code-review-page flex-col gap-lg h-full">
      <header class="page-header">
        <h1>New Code Review</h1>
        <p class="text-secondary">Analyze your source code with AI</p>
      </header>

      <div class="workspace flex gap-lg">
        <div class="editor-section flex-col w-full gap-sm">
          <div class="toolbar flex items-center justify-between">
            <div class="flex items-center gap-sm">
              <select class="select-lang">
                <option>Java</option>
                <option>TypeScript</option>
                <option>Python</option>
                <option>C#</option>
              </select>
            </div>
            <div class="flex items-center gap-sm">
              <button class="btn btn-ghost">Format</button>
              <button class="btn btn-ghost">Clear</button>
            </div>
          </div>
          
          <div class="editor-container">
            <div class="line-numbers flex-col">
              <span *ngFor="let i of [1,2,3,4,5,6,7,8,9,10,11,12]">{{i}}</span>
            </div>
            <textarea class="code-input font-mono" placeholder="Paste your code here..." spellcheck="false"
>public class User {
    private String id;
    private String name;
    
    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    // TODO: implement getters and setters
}</textarea>
          </div>
        </div>

        <div class="config-panel flex-col gap-md">
          <div class="panel-section">
            <h3>Review Configuration</h3>
            <div class="options-list flex-col gap-sm">
              <label class="checkbox-label">
                <input type="checkbox" checked /> Code Quality
              </label>
              <label class="checkbox-label">
                <input type="checkbox" checked /> Bug Detection
              </label>
              <label class="checkbox-label">
                <input type="checkbox" checked /> Security Vulnerabilities
              </label>
              <label class="checkbox-label">
                <input type="checkbox" /> Refactoring Suggestions
              </label>
              <label class="checkbox-label">
                <input type="checkbox" /> Generate Unit Tests
              </label>
            </div>
          </div>
          
          <div class="panel-section flex-col gap-sm">
            <p class="text-muted text-sm">AI will analyze your code and provide structured recommendations based on the selected criteria.</p>
            <button class="btn btn-primary w-full" (click)="analyze()">Analyze Code</button>
          </div>
          
          <div *ngIf="isAnalyzing" class="loading-state flex-col items-center gap-sm">
             <div class="spinner"></div>
             <span class="text-secondary">AI is analyzing your code...</span>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .workspace {
      height: calc(100vh - 160px);
    }
    .editor-section {
      flex: 3;
      min-width: 0;
    }
    .config-panel {
      flex: 1;
      min-width: 250px;
      background-color: var(--bg-panel);
      border: 1px solid var(--bg-panel-border);
      border-radius: var(--border-radius-lg);
      padding: var(--spacing-md);
      height: fit-content;
    }
    .toolbar {
      background-color: var(--bg-panel);
      padding: 8px 16px;
      border-radius: var(--border-radius-md) var(--border-radius-md) 0 0;
      border: 1px solid var(--bg-panel-border);
      border-bottom: none;
    }
    .editor-container {
      flex: 1;
      display: flex;
      background-color: #010409;
      border: 1px solid var(--bg-panel-border);
      border-radius: 0 0 var(--border-radius-md) var(--border-radius-md);
      overflow: hidden;
    }
    .line-numbers {
      padding: 16px 8px;
      background-color: var(--bg-panel);
      color: var(--text-muted);
      font-family: 'Fira Code', monospace;
      font-size: 14px;
      line-height: 1.5;
      text-align: right;
      user-select: none;
      border-right: 1px solid var(--bg-panel-border);
    }
    .code-input {
      flex: 1;
      background: transparent;
      border: none;
      color: #e6edf3;
      padding: 16px;
      font-size: 14px;
      line-height: 1.5;
      resize: none;
    }
    .code-input:focus { outline: none; box-shadow: none; border: none; }
    
    .panel-section {
      padding-bottom: var(--spacing-md);
      border-bottom: 1px solid var(--bg-panel-border);
    }
    .panel-section:last-child { border-bottom: none; padding-bottom: 0; }
    
    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--text-primary);
      font-size: 14px;
      cursor: pointer;
    }
    .text-sm { font-size: 12px; }
    
    .btn {
      padding: 8px 16px;
      border-radius: var(--border-radius-md);
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
    }
    .btn-ghost {
      background: transparent;
      color: var(--text-secondary);
    }
    .btn-ghost:hover {
      background: var(--bg-panel-hover);
      color: var(--text-primary);
    }
    .btn-primary {
      background-color: var(--accent-success);
      color: white;
    }
    .btn-primary:hover { background-color: #2c974b; }
    
    .spinner {
      width: 24px;
      height: 24px;
      border: 3px solid var(--bg-panel-border);
      border-top-color: var(--accent-primary);
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .loading-state { padding: var(--spacing-md) 0; }
  `]
})
export class CodeReview {
  isAnalyzing = false;
  
  analyze() {
    this.isAnalyzing = true;
    setTimeout(() => this.isAnalyzing = false, 2000);
  }
}
