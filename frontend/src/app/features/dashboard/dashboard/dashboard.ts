import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReviewService } from '../../../core/services/review.service';
import { ReviewResponse, ReviewStatus } from '../../../core/models/review.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="dashboard-page flex-col gap-lg">
      <header class="page-header">
        <h1>Dashboard</h1>
        <p class="text-secondary">Monitor your code quality and AI-powered reviews.</p>
      </header>

      <section class="stats-grid grid grid-cols-4 gap-md">
        <div class="stat-card">
          <div class="stat-title">Total Reviews</div>
          <div class="stat-value" *ngIf="!loading">{{ summary.totalReviews }}</div>
          <div class="stat-value" *ngIf="loading">...</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">Completed</div>
          <div class="stat-value text-success" *ngIf="!loading">{{ summary.completedReviews }}</div>
          <div class="stat-value" *ngIf="loading">...</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">Pending Analysis</div>
          <div class="stat-value text-medium" *ngIf="!loading">{{ summary.pendingReviews }}</div>
          <div class="stat-value" *ngIf="loading">...</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">Failed</div>
          <div class="stat-value text-critical" *ngIf="!loading">{{ summary.failedReviews }}</div>
          <div class="stat-value" *ngIf="loading">...</div>
        </div>
      </section>

      <div *ngIf="error" class="error-banner">{{ error }}</div>

      <section class="recent-reviews flex-col gap-md">
        <div class="section-header flex justify-between items-center">
          <h2>Recent Reviews</h2>
          <a routerLink="/history" class="btn btn-outline">View All</a>
        </div>
        
        <div class="table-container">
          <table class="data-table w-full">
            <thead>
              <tr>
                <th>Project</th>
                <th>Language</th>
                <th>Date</th>
                <th>Type</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of history">
                <td class="font-medium"><a [routerLink]="['/reviews', item.id]">{{ item.projectName }}</a></td>
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
              </tr>
              <tr *ngIf="history.length === 0 && !loading">
                <td colspan="5" class="text-center py-4 text-secondary">No reviews found.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .stat-card {
      background-color: var(--bg-panel);
      border: 1px solid var(--bg-panel-border);
      border-radius: var(--border-radius-lg);
      padding: var(--spacing-lg);
      display: flex;
      flex-direction: column;
      gap: var(--spacing-sm);
    }
    .stat-title { color: var(--text-secondary); font-size: 14px; font-weight: 500; }
    .stat-value { font-size: 32px; font-weight: 700; }
    .text-success { color: var(--accent-success); }
    .text-critical { color: var(--accent-danger); }
    .text-medium { color: var(--accent-warning); }
    .error-banner { background-color: rgba(248, 113, 113, 0.1); color: #f87171; padding: 1rem; border-radius: 4px; border: 1px solid rgba(248, 113, 113, 0.2); margin-top: 1rem; }
    
    .table-container { background-color: var(--bg-panel); border: 1px solid var(--bg-panel-border); border-radius: var(--border-radius-lg); overflow: hidden; }
    .data-table { border-collapse: collapse; text-align: left; }
    .data-table th { background-color: rgba(255,255,255,0.02); padding: 12px 16px; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); border-bottom: 1px solid var(--bg-panel-border); }
    .data-table td { padding: 16px; border-bottom: 1px solid var(--bg-panel-border); font-size: 14px; }
    .data-table tr:last-child td { border-bottom: none; }
    .data-table tr:hover td { background-color: var(--bg-panel-hover); }
    .font-medium { font-weight: 500; }
    .font-medium a { color: var(--text-primary); text-decoration: none; }
    .font-medium a:hover { color: var(--accent-primary); text-decoration: underline; }
    
    .badge { padding: 4px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }
    .badge-outline { border: 1px solid var(--bg-panel-border); color: var(--text-secondary); }
    .badge-success { background: rgba(46, 160, 67, 0.15); color: var(--accent-success); }
    .badge-warning { background: rgba(210, 153, 34, 0.15); color: var(--accent-warning); }
    .badge-danger { background: rgba(248, 81, 73, 0.15); color: var(--accent-danger); }
    
    .btn { padding: 6px 12px; border-radius: var(--border-radius-md); font-size: 14px; font-weight: 500; cursor: pointer; display: inline-block; text-align: center; }
    .btn-outline { border: 1px solid var(--bg-panel-border); color: var(--text-primary); background: transparent; text-decoration: none; }
    .btn-outline:hover { background: var(--bg-panel-hover); border-color: var(--text-muted); text-decoration: none; }
    .py-4 { padding-top: 1rem; padding-bottom: 1rem; }
    .text-center { text-align: center; }
  `]
})
export class Dashboard implements OnInit {
  summary = { totalReviews: 0, completedReviews: 0, pendingReviews: 0, failedReviews: 0 };
  history: ReviewResponse[] = [];
  loading = true;
  error = '';

  constructor(
    private reviewService: ReviewService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.fetchData();
  }

  fetchData() {
    this.reviewService.getReviews().subscribe({
      next: (data) => {
        this.loading = false;
        this.summary.totalReviews = data.length;
        this.summary.completedReviews = data.filter(r => r.status === 'COMPLETED').length;
        this.summary.pendingReviews = data.filter(r => r.status === 'PENDING' || r.status === 'ANALYZING').length;
        this.summary.failedReviews = data.filter(r => r.status === 'FAILED').length;
        
        this.history = data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()).slice(0, 5);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        this.error = 'Failed to load dashboard data from backend API.';
        this.cdr.markForCheck();
        console.error(err);
      }
    });
  }
}

