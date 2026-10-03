import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AnalysisSummary } from '../../../core/models/analysis.model';
import { AnalysisService } from '../../../core/services/analysis.service';
import { errorMessage } from '../../../core/services/api-error';

@Component({
  selector: 'app-analysis-list',
  standalone: true,
  imports: [DatePipe, RouterLink],
  template: `
    <div class="flex-col gap-lg">
      <header class="flex justify-between items-center">
        <div><h1>Repository analyses</h1><p class="text-secondary">History of every full-codebase analysis.</p></div>
        <a class="btn btn-primary" routerLink="/repositories">New analysis</a>
      </header>
      @if (error()) { <div class="error-banner" role="alert">{{ error() }}</div> }
      <div class="table-container">
        <table class="data-table">
          <thead><tr><th>Repository</th><th>Source</th><th>Branch</th><th>Commit</th><th>Status</th><th>Score</th><th>Findings</th><th>Date</th><th></th></tr></thead>
          <tbody>
            @for (a of items(); track a.id) {
              <tr>
                <td><a [routerLink]="['/analyses', a.id]"><strong>{{ a.name }}</strong></a></td>
                <td><span class="badge badge-outline">{{ a.sourceType }}</span></td>
                <td>{{ a.branch || '—' }}</td>
                <td class="mono">{{ a.commitHash ? a.commitHash.substring(0, 7) : '—' }}</td>
                <td><span class="badge" [class.badge-success]="a.status === 'COMPLETED'" [class.badge-warning]="a.status === 'ANALYZING'" [class.badge-danger]="a.status === 'FAILED'">{{ a.status }}</span></td>
                <td>{{ a.qualityScore || '—' }}</td>
                <td>{{ a.findingsCount }}</td>
                <td class="text-secondary">{{ a.createdAt | date: 'short' }}</td>
                <td><button class="btn btn-danger btn-sm" (click)="remove(a)">Delete</button></td>
              </tr>
            } @empty {
              <tr><td colspan="9" class="text-center text-secondary">{{ loading() ? 'Loading…' : 'No analysis yet. Start one from Repositories.' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class AnalysisList implements OnInit {
  private readonly api = inject(AnalysisService);
  readonly items = signal<AnalysisSummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.history().subscribe({
      next: (r) => {
        this.items.set(r);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  remove(a: AnalysisSummary): void {
    if (!confirm(`Delete the analysis of ${a.name}?`)) return;
    this.api.delete(a.id).subscribe({ next: () => this.reload(), error: (e) => this.error.set(errorMessage(e)) });
  }
}
