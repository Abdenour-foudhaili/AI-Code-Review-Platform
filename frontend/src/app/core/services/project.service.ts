import { environment } from '../../../environments/environment';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProjectResponse, ProjectDetailsResponse } from '../models/project.model';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private apiUrl = `${environment.apiUrl}/projects`;

  constructor(private http: HttpClient) {}

  getAllProjects(): Observable<ProjectResponse[]> {
    return this.http.get<ProjectResponse[]>(this.apiUrl);
  }

  getProjectById(id: number): Observable<ProjectDetailsResponse> {
    return this.http.get<ProjectDetailsResponse>(`${this.apiUrl}/${id}`);
  }

  createProject(name: string, description: string, file: File): Observable<ProjectResponse> {
    const formData = new FormData();
    formData.append('name', name);
    formData.append('description', description);
    formData.append('file', file);

    return this.http.post<ProjectResponse>(this.apiUrl, formData);
  }

  analyzeProject(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/analyze`, {});
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}