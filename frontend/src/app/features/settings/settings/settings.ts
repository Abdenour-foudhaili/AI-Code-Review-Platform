import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/auth/auth.service';
import { User } from '../../../core/models/auth.model';
import { GithubStatus } from '../../../core/models/analysis.model';
import { errorMessage } from '../../../core/services/api-error';
import { GithubService } from '../../../core/services/github.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [DatePipe],
  template: `
    <div class="flex-col gap-lg">
      <header><h1>Settings</h1><p class="text-secondary">Your account and integrations.</p></header>
      @if (error()) { <div class="error-banner" role="alert">{{ error() }}</div> }
      @if (githubError()) { <div class="error-banner" role="alert">GitHub connection failed. Please try again.</div> }

      <div class="grid grid-cols-2 gap-lg">
        <section class="card flex-col gap-md">
          <h2 style="margin:0">Profile</h2>
          @if (auth.user(); as u) {
            <div class="form-group"><label>Username</label><input [value]="u.username" disabled /></div>
            <div class="form-group"><label>Email</label><input [value]="u.email" disabled /></div>
            <div class="form-group"><label>Role</label><input [value]="u.role" disabled /></div>
          }
          <div><button class="btn" (click)="auth.logout()">Log out</button></div>
        </section>

        <section class="card flex-col gap-md">
          <h2 style="margin:0">GitHub</h2>
          @if (github(); as g) {
            @if (g.connected) {
              <div class="flex items-center gap-sm">
                @if (g.avatarUrl) { <img [src]="g.avatarUrl" alt="" width="36" height="36" style="border-radius:50%" /> }
                <div><strong>{{ g.login }}</strong><div class="text-secondary" style="font-size:12px">Connected {{ g.connectedAt | date: 'medium' }}</div></div>
              </div>
              <p class="text-secondary">The access token is stored encrypted on the server and never sent to the browser.</p>
              <div><button class="btn btn-danger" (click)="disconnect()">Disconnect</button></div>
            } @else {
              <p class="text-secondary">Not connected.</p>
              @if (!g.configured) { <div class="warn-banner">OAuth is not configured on the server (GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET).</div> }
              <div><button class="btn btn-primary" (click)="connect()" [disabled]="!g.configured">Connect GitHub</button></div>
            }
          }
        </section>
      </div>

      @if (auth.isAdmin()) {
        <section class="flex-col gap-md">
          <h2 style="margin:0">Users (admin)</h2>
          <div class="table-container">
            <table class="data-table">
              <thead><tr><th>Username</th><th>Email</th><th>Role</th><th>Enabled</th><th></th></tr></thead>
              <tbody>
                @for (u of users(); track u.id) {
                  <tr><td>{{ u.username }}</td><td>{{ u.email }}</td><td>{{ u.role }}</td><td>{{ u.enabled ? 'Yes' : 'No' }}</td>
                    <td><button class="btn btn-sm" [disabled]="u.id === auth.user()?.id" (click)="toggleUser(u)">{{ u.enabled ? 'Disable' : 'Enable' }}</button></td></tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }
    </div>
  `,
})
export class Settings implements OnInit {
  readonly auth = inject(AuthService);
  private readonly githubApi = inject(GithubService);
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);

  readonly github = signal<GithubStatus | null>(null);
  readonly users = signal<User[]>([]);
  readonly error = signal('');
  readonly githubError = signal(false);

  ngOnInit(): void {
    this.githubError.set(this.route.snapshot.queryParamMap.get('github') === 'error');
    this.githubApi.status().subscribe({ next: (s) => this.github.set(s), error: (e) => this.error.set(errorMessage(e)) });
    if (this.auth.isAdmin()) this.loadUsers();
  }

  connect(): void {
    this.githubApi.connectUrl().subscribe({ next: (r) => (window.location.href = r.url), error: (e) => this.error.set(errorMessage(e)) });
  }

  disconnect(): void {
    this.githubApi.disconnect().subscribe({
      next: () => this.githubApi.status().subscribe((s) => this.github.set(s)),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  private loadUsers(): void {
    this.http.get<User[]>(`${environment.apiUrl}/admin/users`).subscribe({ next: (u) => this.users.set(u), error: (e) => this.error.set(errorMessage(e)) });
  }

  toggleUser(u: User): void {
    this.http.put<User>(`${environment.apiUrl}/admin/users/${u.id}`, { enabled: !u.enabled }).subscribe({
      next: () => this.loadUsers(),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
