import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardData } from '../../../core/models/analysis.model';
import { errorMessage } from '../../../core/services/api-error';
import { DashboardService } from '../../../core/services/rules.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink],
  template: `
    <div class="flex-col gap-lg">
      <header class="flex justify-between items-center">
        <div><h1>Dashboard</h1><p class="text-secondary">Your repositories, analyses and findings.</p></div>
        <a class="btn btn-primary" routerLink="/repositories">Analyze a repository</a>
      </header>
      @if (error()) { <div class="error-banner" role="alert">{{ error() }}</div> }

      @if (data(); as d) {
        @if (!d.github.connected) {
          <div class="info-banner">GitHub is not connected yet. <a routerLink="/repositories">Connect GitHub</a> to analyse your repositories.</div>
        }
        <section class="grid grid-cols-4 gap-md">
          <div class="card"><div class="text-secondary">Repositories</div><div class="big">{{ d.repositories }}</div></div>
          <div class="card"><div class="text-secondary">Analyses</div><div class="big">{{ d.analyses }}</div><div class="text-secondary" style="font-size:12px">{{ d.completedAnalyses }} completed</div></div>
          <div class="card"><div class="text-secondary">Critical findings</div><div class="big text-critical">{{ d.criticalFindings }}</div></div>
          <div class="card"><div class="text-secondary">Security findings</div><div class="big text-high">{{ d.securityFindings }}</div></div>
        </section>

        <section class="grid grid-cols-2 gap-lg">
          <div class="card flex-col gap-sm">
            <h3>Last analysis</h3>
            @if (d.lastAnalysis; as l) {
              <div><a [routerLink]="['/analyses', l.id]"><strong>{{ l.name }}</strong></a> · score {{ l.qualityScore || '—' }} · {{ l.completedAt | date: 'medium' }}</div>
              <p class="text-secondary" style="white-space:pre-line">{{ l.summary }}</p>
            } @else { <p class="text-secondary">No completed analysis yet.</p> }
          </div>
          <div class="card flex-col gap-sm">
            <h3>Architecture overview</h3>
            @for (s of d.architectureStyles; track s) { <div>✓ {{ s }}</div> }
            <div>@for (r of roles(d); track r[0]) { <span class="chip">{{ r[0] }}: {{ r[1] }}</span> }</div>
            <div>@for (t of d.technologies; track t) { <span class="chip">{{ t }}</span> }</div>
            @if (!d.architectureStyles.length) { <p class="text-secondary">Available after the first analysis.</p> }
          </div>
        </section>

        <section class="flex-col gap-md">
          <div class="flex justify-between items-center"><h2 style="margin:0">Recent analyses</h2><a class="btn btn-sm" routerLink="/analyses">View all</a></div>
          <div class="table-container">
            <table class="data-table">
              <thead><tr><th>Name</th><th>Source</th><th>Status</th><th>Score</th><th>Date</th></tr></thead>
              <tbody>
                @for (a of d.recentAnalyses; track a.id) {
                  <tr><td><a [routerLink]="['/analyses', a.id]">{{ a.name }}</a></td><td><span class="badge badge-outline">{{ a.sourceType }}</span></td>
                    <td><span class="badge" [class.badge-success]="a.status === 'COMPLETED'" [class.badge-warning]="a.status === 'ANALYZING'" [class.badge-danger]="a.status === 'FAILED'">{{ a.status }}</span></td>
                    <td>{{ a.qualityScore || '—' }}</td><td class="text-secondary">{{ a.createdAt | date: 'short' }}</td></tr>
                } @empty { <tr><td colspan="5" class="text-center text-secondary">No analysis yet.</td></tr> }
              </tbody>
            </table>
          </div>
          <p class="text-secondary" style="font-size:13px">Single-file reviews: {{ d.singleFileReviews }} · <a routerLink="/history">history</a> · <a routerLink="/reviews/new">new code review</a></p>
        </section>
      } @else if (!error()) { <p class="text-secondary">Loading…</p> }
    </div>
  `,
  styles: [`.big { font-size: 32px; font-weight: 700; }`],
})
export class Dashboard implements OnInit {
  private readonly api = inject(DashboardService);
  readonly data = signal<DashboardData | null>(null);
  readonly error = signal('');

  ngOnInit(): void {
    this.api.get().subscribe({ next: (d) => this.data.set(d), error: (e) => this.error.set(errorMessage(e, 'Failed to load the dashboard')) });
  }

  roles(d: DashboardData): [string, number][] {
    return Object.entries(d.architectureRoles);
  }
}
