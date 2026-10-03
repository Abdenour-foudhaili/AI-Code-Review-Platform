import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AnalysisDetails, AnalysisStatus, AnalysisSummary, CodeContext, Diagram, Finding } from '../models/analysis.model';

export interface FindingFilter {
  category?: string;
  severity?: string;
  file?: string;
  source?: string;
}

@Injectable({ providedIn: 'root' })
export class AnalysisService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/analyses`;

  start(owner: string, repo: string, branch?: string): Observable<{ analysisId: number; status: string }> {
    return this.http.post<{ analysisId: number; status: string }>(this.base, { owner, repo, branch: branch || null });
  }

  startZip(projectId: number): Observable<{ analysisId: number; status: string }> {
    return this.http.post<{ analysisId: number; status: string }>(`${this.base}/zip/${projectId}`, {});
  }

  history(): Observable<AnalysisSummary[]> {
    return this.http.get<AnalysisSummary[]>(this.base);
  }

  details(id: number): Observable<AnalysisDetails> {
    return this.http.get<AnalysisDetails>(`${this.base}/${id}`);
  }

  status(id: number): Observable<AnalysisStatus> {
    return this.http.get<AnalysisStatus>(`${this.base}/${id}/status`);
  }

  findings(id: number, filter: FindingFilter = {}): Observable<Finding[]> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filter)) {
      if (value) params = params.set(key, value);
    }
    return this.http.get<Finding[]>(`${this.base}/${id}/findings`, { params });
  }

  diagrams(id: number): Observable<{ diagrams: Diagram[] }> {
    return this.http.get<{ diagrams: Diagram[] }>(`${this.base}/${id}/diagrams`);
  }

  architecture(id: number): Observable<unknown> {
    return this.http.get<unknown>(`${this.base}/${id}/architecture`);
  }

  code(id: number, path: string, line: number, radius = 12): Observable<CodeContext> {
    return this.http.get<CodeContext>(`${this.base}/${id}/code`, { params: { path, line, radius } });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
