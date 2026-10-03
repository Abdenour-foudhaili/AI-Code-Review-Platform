import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { GithubRepo, GithubStatus } from '../../core/models/analysis.model';
import { AnalysisService } from '../../core/services/analysis.service';
import { errorMessage } from '../../core/services/api-error';
import { GithubService } from '../../core/services/github.service';

@Component({
  selector: 'app-repositories',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink],
  template: `
    <div class="flex-col gap-lg">
      <header class="flex justify-between items-center">
        <div>
          <h1>Repositories</h1>
          <p class="text-secondary">Pick a GitHub repository and run a full-codebase analysis.</p>
        </div>
        @if (status()?.connected) {
          <div class="flex items-center gap-sm text-secondary">Connected as <strong>{{ status()?.login }}</strong></div>
        }
      </header>

      @if (justConnected()) { <div class="info-banner">GitHub connected successfully.</div> }
      @if (error()) { <div class="error-banner" role="alert">{{ error() }}</div> }

      @if (status() && !status()!.connected) {
        <div class="card flex-col gap-md">
          <h3>Connect your GitHub account</h3>
          <p class="text-secondary">Authorise the app through GitHub OAuth to list your repositories. Your token stays on the server, encrypted.</p>
          @if (!status()!.configured) { <div class="warn-banner">GitHub OAuth is not configured on the server (GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET).</div> }
          <div><button class="btn btn-primary" (click)="connect()" [disabled]="!status()!.configured || connecting()">Connect GitHub</button></div>
        </div>
      } @else if (status()?.connected) {
        <div class="flex gap-sm">
          <input type="search" placeholder="Filter repositories…" [ngModel]="filter()" (ngModelChange)="filter.set($event)" style="flex:1" />
          <button class="btn" (click)="load()" [disabled]="loading()">Refresh</button>
        </div>
        <div class="table-container">
          @if (loading()) { <div class="p-lg text-center text-secondary">Loading repositories…</div> }
          @else {
            <table class="data-table">
              <thead><tr><th>Repository</th><th>Owner</th><th>Language</th><th>Stars</th><th>Visibility</th><th>Updated</th><th></th></tr></thead>
              <tbody>
                @for (r of visible(); track r.id) {
                  <tr>
                    <td><strong>{{ r.name }}</strong><div class="text-secondary" style="font-size:12px">{{ r.description }}</div></td>
                    <td>{{ r.owner }}</td>
                    <td>{{ r.language || '—' }}</td>
                    <td>★ {{ r.stars }}</td>
                    <td><span class="badge badge-outline">{{ r.isPrivate ? 'Private' : 'Public' }}</span></td>
                    <td class="text-secondary">{{ r.updatedAt | date: 'mediumDate' }}</td>
                    <td><button class="btn btn-primary btn-sm" (click)="analyze(r)" [disabled]="starting() === r.id">
                      {{ starting() === r.id ? 'Starting…' : 'Analyze Repository' }}</button></td>
                  </tr>
                } @empty {
                  <tr><td colspan="7" class="text-center text-secondary">No repository found.</td></tr>
                }
              </tbody>
            </table>
          }
        </div>
        <p class="text-secondary" style="font-size:13px">Previous results: <a routerLink="/analyses">analysis history</a></p>
      }
    </div>
  `,
})
export class Repositories implements OnInit {
  private readonly github = inject(GithubService);
  private readonly analyses = inject(AnalysisService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly status = signal<GithubStatus | null>(null);
  readonly repos = signal<GithubRepo[]>([]);
  readonly filter = signal('');
  readonly loading = signal(false);
  readonly connecting = signal(false);
  readonly starting = signal<number | null>(null);
  readonly error = signal('');
  readonly justConnected = signal(false);

  readonly visible = computed(() => {
    const q = this.filter().trim().toLowerCase();
    return q ? this.repos().filter((r) => r.fullName.toLowerCase().includes(q) || (r.description ?? '').toLowerCase().includes(q)) : this.repos();
  });

  ngOnInit(): void {
    this.justConnected.set(this.route.snapshot.queryParamMap.get('github') === 'connected');
    this.github.status().subscribe({
      next: (s) => {
        this.status.set(s);
        if (s.connected) this.load();
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.github.repositories().subscribe({
      next: (r) => {
        this.repos.set(r);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(errorMessage(e, 'Could not load repositories'));
      },
    });
  }

  connect(): void {
    this.connecting.set(true);
    this.github.connectUrl().subscribe({
      next: (r) => (window.location.href = r.url),
      error: (e) => {
        this.connecting.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  analyze(repo: GithubRepo): void {
    this.starting.set(repo.id);
    this.error.set('');
    this.analyses.start(repo.owner, repo.name, repo.defaultBranch).subscribe({
      next: (r) => void this.router.navigate(['/analyses', r.analysisId]),
      error: (e) => {
        this.starting.set(null);
        this.error.set(errorMessage(e, 'Could not start the analysis'));
      },
    });
  }
}
