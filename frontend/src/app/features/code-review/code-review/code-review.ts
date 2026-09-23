import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ReviewService } from '../../../core/services/review.service';
import { CreateReviewRequest, ReviewType } from '../../../core/models/review.model';

@Component({
  selector: 'app-code-review',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="code-review-page flex-col gap-lg h-full">
      <header class="page-header">
        <h1>New Code Review</h1>
        <p class="text-secondary">Submit your source code for an AI-powered code review.</p>
      </header>
      
      <div *ngIf="error" class="error-banner">{{ error }}</div>

      <div class="review-layout flex gap-lg flex-1">
        
        <div class="editor-section flex-col gap-sm flex-2">
          <div class="section-title">Source Code</div>
          <div class="editor-container">
            <textarea 
              [(ngModel)]="request.sourceCode"
              class="code-editor" 
              placeholder="// Paste your source code here..." 
              spellcheck="false"
              [disabled]="loading">
            </textarea>
          </div>
        </div>

        <div class="config-section flex-col gap-md flex-1">
          <div class="config-card flex-col gap-md">
            <h3>Configuration</h3>
            
            <div class="form-group flex-col gap-xs">
              <label>Project Name</label>
              <input type="text" [(ngModel)]="request.projectName" class="form-control" placeholder="e.g. Authentication Service" [disabled]="loading" />
            </div>

            <div class="form-group flex-col gap-xs">
              <label>Programming Language</label>
              <select class="form-control" [(ngModel)]="request.language" [disabled]="loading">
                <option value="Java">Java</option>
                <option value="TypeScript">TypeScript</option>
                <option value="Python">Python</option>
                <option value="C#">C#</option>
                <option value="Go">Go</option>
              </select>
            </div>

            <div class="form-group flex-col gap-xs">
              <label>Review Type</label>
              <select class="form-control" [(ngModel)]="request.reviewType" [disabled]="loading">
                <option value="FULL_REVIEW">Full Review</option>
                <option value="BUG_DETECTION">Bug Detection</option>
                <option value="SECURITY">Security Analysis</option>
                <option value="CODE_QUALITY">Code Quality</option>
              </select>
            </div>

            <div class="action-area mt-auto pt-lg">
              <button class="btn btn-primary w-full btn-lg" (click)="submitReview()" [disabled]="loading || !isValid()">
                <span class="icon" *ngIf="!loading">✨</span> 
                {{ loading ? 'Submitting...' : 'Analyze Code' }}
              </button>
            </div>
          </div>
        </div>

      </div>
    </div>
  `,
  styles: [`
    .review-layout { min-height: 500px; display: flex; align-items: stretch; }
    .flex-1 { flex: 1; }
    .flex-2 { flex: 2; }
    .h-full { height: 100%; }
    .mt-auto { margin-top: auto; }
    .pt-lg { padding-top: var(--spacing-lg); }
    .w-full { width: 100%; }

    .section-title { font-weight: 600; color: var(--text-secondary); text-transform: uppercase; font-size: 12px; letter-spacing: 0.05em; }
    
    .editor-container {
      flex: 1; display: flex; background-color: var(--bg-body);
      border: 1px solid var(--bg-panel-border); border-radius: var(--border-radius-md);
      overflow: hidden;
    }
    .code-editor {
      width: 100%; padding: var(--spacing-md); background: transparent; color: var(--text-primary);
      border: none; outline: none; resize: none; font-family: 'Fira Code', 'Consolas', monospace;
      font-size: 14px; line-height: 1.5;
    }
    .code-editor:focus { box-shadow: inset 0 0 0 1px var(--accent-primary); }

    .config-card {
      background-color: var(--bg-panel); border: 1px solid var(--bg-panel-border);
      border-radius: var(--border-radius-lg); padding: var(--spacing-lg); flex: 1;
    }
    .config-card h3 { font-size: 16px; font-weight: 600; border-bottom: 1px solid var(--bg-panel-border); padding-bottom: var(--spacing-sm); margin-bottom: var(--spacing-sm); }
    
    .form-group label { font-size: 13px; color: var(--text-secondary); font-weight: 500; }
    .form-control {
      background-color: var(--bg-body); border: 1px solid var(--bg-panel-border);
      color: var(--text-primary); padding: 10px 12px; border-radius: var(--border-radius-sm);
      font-size: 14px; outline: none; transition: border-color 0.2s ease; width: 100%;
    }
    .form-control:focus { border-color: var(--accent-primary); }
    
    .btn { padding: 8px 16px; border-radius: var(--border-radius-md); font-weight: 500; cursor: pointer; border: none; display: flex; align-items: center; justify-content: center; gap: 8px; transition: all 0.2s ease; }
    .btn-primary { background: linear-gradient(135deg, var(--accent-primary), #8b5cf6); color: white; }
    .btn-primary:hover { opacity: 0.9; box-shadow: 0 4px 12px rgba(88, 166, 255, 0.25); }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-lg { padding: 12px 24px; font-size: 16px; font-weight: 600; }
    .error-banner { background-color: rgba(248, 113, 113, 0.1); color: #f87171; padding: 1rem; border-radius: 4px; border: 1px solid rgba(248, 113, 113, 0.2); }
  `]
})
export class CodeReview {
  request: CreateReviewRequest = {
    projectName: '',
    language: 'Java',
    sourceCode: '',
    reviewType: ReviewType.FULL_REVIEW
  };
  loading = false;
  error = '';

  constructor(
    private reviewService: ReviewService, 
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  isValid(): boolean {
    return !!(this.request.projectName && this.request.sourceCode && this.request.language);
  }

  submitReview() {
    if (!this.isValid()) return;
    this.loading = true;
    this.error = '';
    
    this.reviewService.createReview(this.request).subscribe({
      next: (response) => {
        this.loading = false;
        this.router.navigate(['/reviews', response.id]);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Failed to submit review.';
        this.cdr.markForCheck();
        console.error(err);
      }
    });
  }
}
