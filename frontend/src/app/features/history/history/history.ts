import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReviewService } from '../../../core/services/review.service';
import { ReviewResponse } from '../../../core/models/review.model';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="history-page flex-col gap-lg">
      <header class="page-header flex justify-between items-center">
        <div>
          <h1>Review History</h1>
          <p class="text-secondary">Track and revisit your past AI code reviews.</p>
        </div>
        <div class="filters flex gap-sm">
          <button class="btn btn-outline" (click)="loadReviews()" [disabled]="loading">Refresh</button>
        </div>
      </header>
      
      <div *ngIf="error" class="error-banner mb-4">{{ error }}</div>

      <div class="table-container">
        <div *ngIf="loading" class="p-4 text-center text-secondary">Loading reviews...</div>
        <table class="data-table w-full" *ngIf="!loading">
          <thead>
            <tr>
              <th>ID</th>
              <th>Project</th>
              <th>Language</th>
              <th>Date</th>
              <th>Type</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let item of history">
              <td class="text-secondary">#{{ item.id }}</td>
              <td class="font-medium">{{ item.projectName }}</td>
              <td><span class="badge badge-outline">{{ item.language }}</span></td>
              <td class="text-secondary">{{ item.createdAt | date:'short' }}</td>
              <td><span class="badge badge-outline">{{ item.reviewType }}</span></td>
              <td>
                <span class="badge" 
                      [class.badge-success]="item.status === 'COMPLETED'"
                      [class.badge-warning]="item.status === 'PENDING' || item.status === 'ANALYZING'"
                      [class.badge-danger]="item.status === 'FAILED'">
                  {{ item.status }}
                </span>
              </td>
              <td>
                <a [routerLink]="['/reviews', item.id]" class="btn btn-ghost btn-sm">View</a>
                <button class="btn btn-ghost btn-sm text-critical" (click)="deleteReview(item.id)">Delete</button>
              </td>
            </tr>
            <tr *ngIf="history.length === 0">
              <td colspan="7" class="text-center py-4 text-secondary">No reviews found in history.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .filters { align-items: center; }
    
    .table-container { background-color: var(--bg-panel); border: 1px solid var(--bg-panel-border); border-radius: var(--border-radius-lg); }
    .data-table { border-collapse: collapse; text-align: left; }
    .data-table th { background-color: rgba(255,255,255,0.02); padding: 12px 16px; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); border-bottom: 1px solid var(--bg-panel-border); }
    .data-table td { padding: 16px; border-bottom: 1px solid var(--bg-panel-border); font-size: 14px; }
    .font-medium { font-weight: 500; }
    
    .badge { padding: 4px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }
    .badge-outline { border: 1px solid var(--bg-panel-border); color: var(--text-secondary); }
    .badge-success { background: rgba(46, 160, 67, 0.15); color: var(--accent-success); }
    .badge-warning { background: rgba(210, 153, 34, 0.15); color: var(--accent-warning); }
    .badge-danger { background: rgba(248, 81, 73, 0.15); color: var(--accent-danger); }
    
    .btn { padding: 6px 12px; border-radius: var(--border-radius-md); font-size: 14px; font-weight: 500; cursor: pointer; display: inline-block; text-align: center; border: none; text-decoration: none; }
    .btn-sm { padding: 4px 8px; font-size: 12px; }
    .btn-outline { border: 1px solid var(--bg-panel-border); color: var(--text-primary); background: transparent; }
    .btn-outline:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-ghost { background: transparent; color: var(--accent-primary); }
    .btn-ghost:hover { background: rgba(88, 166, 255, 0.1); }
    .text-critical { color: var(--accent-danger) !important; }
    
    .error-banner { background-color: rgba(248, 113, 113, 0.1); color: #f87171; padding: 1rem; border-radius: 4px; border: 1px solid rgba(248, 113, 113, 0.2); }
    .mb-4 { margin-bottom: 1rem; }
    .p-4 { padding: 1rem; }
    .py-4 { padding-top: 1rem; padding-bottom: 1rem; }
    .text-center { text-align: center; }
  `]
})
export class History implements OnInit {
  history: ReviewResponse[] = [];
  loading = true;
  error = '';

  constructor(
    private reviewService: ReviewService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadReviews();
  }

  loadReviews() {
    this.loading = true;
    this.reviewService.getReviews().subscribe({
      next: (data) => {
        this.history = data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        this.loading = false;
        this.error = '';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = 'Failed to load reviews from API.';
        this.loading = false;
        this.cdr.markForCheck();
        console.error(err);
      }
    });
  }

  deleteReview(id: number) {
    if (confirm('Are you sure you want to delete this review?')) {
      this.reviewService.deleteReview(id).subscribe({
        next: () => {
          this.history = this.history.filter(r => r.id !== id);
          this.cdr.markForCheck();
        },
        error: (err) => {
          alert('Failed to delete review: ' + (err.error?.message || err.message));
          this.cdr.markForCheck();
        }
      });
    }
  }
}
