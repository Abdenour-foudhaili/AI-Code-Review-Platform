import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ProjectService } from '../../../core/services/project.service';
import { ProjectResponse } from '../../../core/models/project.model';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="projects-container">
      <header class="flex justify-between items-center mb-xl">
        <div>
          <h1 class="text-xl font-bold mb-xs">Projects</h1>
          <p class="text-secondary text-sm">Manage and analyze your software projects.</p>
        </div>
        <button class="btn btn-primary" routerLink="/projects/import">+ Import Project</button>
      </header>
      
      <div class="card p-0">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="text-secondary text-sm border-b border-panel">
              <th class="p-md font-medium">Project Name</th>
              <th class="p-md font-medium">Status</th>
              <th class="p-md font-medium">Languages</th>
              <th class="p-md font-medium">Files</th>
              <th class="p-md font-medium">Quality</th>
              <th class="p-md font-medium">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngIf="loading">
              <td colspan="6" class="p-md text-center text-secondary">Loading projects...</td>
            </tr>
            <tr *ngIf="!loading && projects.length === 0">
              <td colspan="6" class="p-md text-center text-secondary">No projects found.</td>
            </tr>
            <tr class="border-b border-panel hover-row" *ngFor="let p of projects">
              <td class="p-md font-medium">{{ p.name }}</td>
              <td class="p-md">
                <span class="badge" [class.badge-success]="p.status==='COMPLETED'" [class.badge-warning]="p.status==='ANALYZING'" [class.badge-info]="p.status==='READY'" [class.badge-danger]="p.status==='FAILED'">{{ p.status }}</span>
              </td>
              <td class="p-md text-secondary text-sm">{{ p.detectedLanguages || 'N/A' }}</td>
              <td class="p-md text-secondary text-sm">{{ p.filesCount }}</td>
              <td class="p-md font-medium" [class.text-success]="p.qualityScore==='A' || p.qualityScore==='B'" [class.text-warning]="p.qualityScore==='C'" [class.text-danger]="p.qualityScore==='D'">{{ p.qualityScore || '-' }}</td>
              <td class="p-md">
                <div class="flex gap-sm">
                  <button class="btn btn-sm btn-outline" [routerLink]="['/projects', p.id]">Details</button>
                  <button class="btn btn-sm btn-primary" (click)="analyze(p.id)" *ngIf="p.status !== 'ANALYZING'">Analyze</button>
                  <button class="btn btn-sm btn-outline text-danger" (click)="deleteProject(p.id)">Delete</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .projects-container { max-width: 1200px; margin: 0 auto; }
    .card { background-color: var(--bg-panel); border: 1px solid var(--bg-panel-border); border-radius: var(--border-radius-lg); overflow: hidden; }
    .p-md { padding: 16px; }
    .border-b { border-bottom: 1px solid var(--bg-panel-border); }
    .hover-row:hover { background-color: var(--bg-panel-hover); }
    .badge { padding: 4px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }
    .badge-success { background: rgba(46, 160, 67, 0.15); color: var(--accent-success); }
    .badge-warning { background: rgba(210, 153, 34, 0.15); color: var(--accent-warning); }
    .badge-danger { background: rgba(248, 81, 73, 0.15); color: var(--accent-danger); }
    .badge-info { background: rgba(88, 166, 255, 0.15); color: var(--accent-primary); }
    .btn { padding: 6px 16px; border-radius: var(--border-radius-md); font-weight: 500; cursor: pointer; border: none; }
    .btn-sm { padding: 4px 10px; font-size: 12px; }
    .btn-primary { background: linear-gradient(135deg, var(--accent-primary), #8b5cf6); color: white; }
    .btn-outline { border: 1px solid var(--bg-panel-border); color: var(--text-primary); background: transparent; }
    .btn-outline:hover { background: var(--bg-panel-hover); }
    .text-danger { color: var(--accent-danger); }
    .text-success { color: var(--accent-success); }
    .text-warning { color: var(--accent-warning); }
  `]
})
export class ProjectList implements OnInit {
  projects: ProjectResponse[] = [];
  loading = true;

  constructor(private projectService: ProjectService, private cdr: ChangeDetectorRef, private router: Router) {}

  ngOnInit() {
    this.loadProjects();
  }

  loadProjects() {
    this.loading = true;
    this.projectService.getAllProjects().subscribe({
      next: (data) => {
        this.projects = data;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  analyze(id: number) {
    this.projectService.analyzeProject(id).subscribe({
      next: () => {
        this.loadProjects();
      },
      error: (err) => {
        alert(err.error?.message || 'Failed');
      }
    });
  }

  deleteProject(id: number) {
    if(confirm('Are you sure?')) {
      this.projectService.deleteProject(id).subscribe({
        next: () => this.loadProjects()
      });
    }
  }
}
