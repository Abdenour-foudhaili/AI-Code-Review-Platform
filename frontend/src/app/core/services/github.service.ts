import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GithubRepo, GithubStatus } from '../models/analysis.model';

@Injectable({ providedIn: 'root' })
export class GithubService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/github`;

  status(): Observable<GithubStatus> {
    return this.http.get<GithubStatus>(`${this.base}/status`);
  }

  connectUrl(): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(`${this.base}/connect-url`);
  }

  repositories(page = 1, perPage = 50): Observable<GithubRepo[]> {
    return this.http.get<GithubRepo[]>(`${this.base}/repositories`, { params: { page, perPage } });
  }

  disconnect(): Observable<void> {
    return this.http.delete<void>(`${this.base}/connection`);
  }
}
