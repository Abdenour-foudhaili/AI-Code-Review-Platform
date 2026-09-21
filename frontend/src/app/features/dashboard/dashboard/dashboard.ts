import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MOCK_REVIEW_SUMMARY, MOCK_HISTORY } from '../../../core/mocks/review.mock';
import { RouterModule } from '@angular/router';

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
          <div class="stat-value">{{ summary.totalReviews }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">Issues Detected</div>
          <div class="stat-value text-medium">{{ summary.issuesDetected }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">Security Issues</div>
          <div class="stat-value text-critical">{{ summary.securityIssues }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">Avg Code Quality</div>
          <div class="stat-value text-success">{{ summary.averageCodeQuality }}</div>
        </div>
      </section>

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
                <th>Issues</th>
                <th>Security</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of history.slice(0, 5)">
                <td class="font-medium">{{ item.project }}</td>
                <td><span class="badge badge-outline">{{ item.language }}</span></td>
                <td class="text-secondary">{{ item.date }}</td>
                <td>
                  <span [class.text-medium]="item.issuesCount > 0">{{ item.issuesCount }}</span>
                </td>
                <td>
                  <span [class.text-critical]="item.securityCount > 0">{{ item.securityCount }}</span>
                </td>
                <td>
                  <span class="badge" 
                        [class.badge-success]="item.status === 'Completed'"
                        [class.badge-warning]="item.status === 'In Progress'"
                        [class.badge-danger]="item.status === 'Failed'">
                    {{ item.status }}
                  </span>
                </td>
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
    .stat-title {
      color: var(--text-secondary);
      font-size: 14px;
      font-weight: 500;
    }
    .stat-value {
      font-size: 32px;
      font-weight: 700;
    }
    .text-success { color: var(--accent-success); }
    
    .table-container {
      background-color: var(--bg-panel);
      border: 1px solid var(--bg-panel-border);
      border-radius: var(--border-radius-lg);
      overflow: hidden;
    }
    .data-table {
      border-collapse: collapse;
      text-align: left;
    }
    .data-table th {
      background-color: rgba(255,255,255,0.02);
      padding: 12px 16px;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      border-bottom: 1px solid var(--bg-panel-border);
    }
    .data-table td {
      padding: 16px;
      border-bottom: 1px solid var(--bg-panel-border);
      font-size: 14px;
    }
    .data-table tr:last-child td {
      border-bottom: none;
    }
    .data-table tr:hover td {
      background-color: var(--bg-panel-hover);
    }
    .font-medium { font-weight: 500; }
    
    .badge {
      padding: 4px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
    }
    .badge-outline {
      border: 1px solid var(--bg-panel-border);
      color: var(--text-secondary);
    }
    .badge-success { background: rgba(46, 160, 67, 0.15); color: var(--accent-success); }
    .badge-warning { background: rgba(210, 153, 34, 0.15); color: var(--accent-warning); }
    .badge-danger { background: rgba(248, 81, 73, 0.15); color: var(--accent-danger); }
    
    .btn {
      padding: 6px 12px;
      border-radius: var(--border-radius-md);
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      display: inline-block;
      text-align: center;
    }
    .btn-outline {
      border: 1px solid var(--bg-panel-border);
      color: var(--text-primary);
      background: transparent;
    }
    .btn-outline:hover {
      background: var(--bg-panel-hover);
      border-color: var(--text-muted);
      text-decoration: none;
    }
  `]
})
export class Dashboard {
  summary = MOCK_REVIEW_SUMMARY;
  history = MOCK_HISTORY;
}
