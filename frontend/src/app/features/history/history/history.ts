import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MOCK_HISTORY } from '../../../core/mocks/review.mock';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="history-page flex-col gap-lg">
      <header class="page-header flex justify-between items-center">
        <div>
          <h1>Review History</h1>
          <p class="text-secondary">Track and revisit your past AI code reviews.</p>
        </div>
        <div class="filters flex gap-sm">
          <input type="text" placeholder="Search reviews..." class="search-input" />
          <select>
            <option>All Languages</option>
            <option>Java</option>
            <option>TypeScript</option>
          </select>
          <select>
            <option>All Statuses</option>
            <option>Completed</option>
            <option>In Progress</option>
            <option>Failed</option>
          </select>
        </div>
      </header>

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
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let item of history">
              <td class="font-medium">{{ item.project }}</td>
              <td><span class="badge badge-outline">{{ item.language }}</span></td>
              <td class="text-secondary">{{ item.date }}</td>
              <td><span [class.text-medium]="item.issuesCount > 0">{{ item.issuesCount }}</span></td>
              <td><span [class.text-critical]="item.securityCount > 0">{{ item.securityCount }}</span></td>
              <td>
                <span class="badge" 
                      [class.badge-success]="item.status === 'Completed'"
                      [class.badge-warning]="item.status === 'In Progress'"
                      [class.badge-danger]="item.status === 'Failed'">
                  {{ item.status }}
                </span>
              </td>
              <td>
                <button class="btn btn-ghost btn-sm">View Results</button>
              </td>
            </tr>
          </tbody>
        </table>
        
        <div class="pagination flex justify-between items-center">
          <span class="text-secondary text-sm">Showing 1 to 5 of 128 entries</span>
          <div class="flex gap-sm">
            <button class="btn btn-outline" disabled>Previous</button>
            <button class="btn btn-outline">Next</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .filters { align-items: center; }
    .search-input { width: 250px; }
    
    .table-container {
      background-color: var(--bg-panel);
      border: 1px solid var(--bg-panel-border);
      border-radius: var(--border-radius-lg);
    }
    .data-table { border-collapse: collapse; text-align: left; }
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
    .font-medium { font-weight: 500; }
    
    .badge { padding: 4px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }
    .badge-outline { border: 1px solid var(--bg-panel-border); color: var(--text-secondary); }
    .badge-success { background: rgba(46, 160, 67, 0.15); color: var(--accent-success); }
    .badge-warning { background: rgba(210, 153, 34, 0.15); color: var(--accent-warning); }
    .badge-danger { background: rgba(248, 81, 73, 0.15); color: var(--accent-danger); }
    
    .btn {
      padding: 6px 12px; border-radius: var(--border-radius-md); font-size: 14px; font-weight: 500;
      cursor: pointer; display: inline-block; text-align: center; border: none;
    }
    .btn-sm { padding: 4px 8px; font-size: 12px; }
    .btn-outline { border: 1px solid var(--bg-panel-border); color: var(--text-primary); background: transparent; }
    .btn-outline:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-ghost { background: transparent; color: var(--accent-primary); }
    .btn-ghost:hover { background: rgba(88, 166, 255, 0.1); }
    
    .pagination { padding: 16px; border-top: 1px solid var(--bg-panel-border); }
    .text-sm { font-size: 12px; }
  `]
})
export class History {
  history = MOCK_HISTORY;
}
