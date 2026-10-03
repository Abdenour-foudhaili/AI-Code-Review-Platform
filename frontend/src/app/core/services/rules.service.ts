import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardData, ReviewRule } from '../models/analysis.model';

export interface RulePayload {
  name: string;
  description: string;
  ruleText: string;
  enabled: boolean;
}

@Injectable({ providedIn: 'root' })
export class RulesService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/rules`;

  list(): Observable<ReviewRule[]> {
    return this.http.get<ReviewRule[]>(this.base);
  }
  create(rule: RulePayload): Observable<ReviewRule> {
    return this.http.post<ReviewRule>(this.base, rule);
  }
  update(id: number, rule: RulePayload): Observable<ReviewRule> {
    return this.http.put<ReviewRule>(`${this.base}/${id}`, rule);
  }
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  get(): Observable<DashboardData> {
    return this.http.get<DashboardData>(`${environment.apiUrl}/dashboard`);
  }
}
