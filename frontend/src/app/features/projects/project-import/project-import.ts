import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ProjectService } from '../../../core/services/project.service';

@Component({
  selector: 'app-project-import',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="projects-container">
      <header class="mb-xl">
        <h1 class="text-xl font-bold mb-xs">Import Project</h1>
        <p class="text-secondary text-sm">Upload a ZIP archive of your project for analysis.</p>
      </header>
      
      <div class="card flex-col gap-md">
        <div class="form-group">
          <label>Project Name</label>
          <input type="text" class="input" [(ngModel)]="name" placeholder="E.g. E-Commerce API" [disabled]="uploading || success">
        </div>
        <div class="form-group">
          <label>Description (Optional)</label>
          <input type="text" class="input" [(ngModel)]="description" placeholder="Project description" [disabled]="uploading || success">
        </div>
        <div class="form-group">
          <label>Project ZIP Archive</label>
          <input type="file" class="input" (change)="onFileSelected($event)" accept=".zip" [disabled]="uploading || success">
          <p class="text-secondary text-sm mt-xs">Select a valid ZIP file. Max size 50MB. Ignored dirs: node_modules, .git, etc.</p>
        </div>
        
        <div class="flex gap-sm mt-md" *ngIf="!success">
          <button class="btn btn-primary" [disabled]="!file || !name || uploading" (click)="upload()">
            {{ uploading ? 'Importing project...' : 'Import Project' }}
          </button>
          <button class="btn btn-outline" routerLink="/projects" [disabled]="uploading">Cancel</button>
        </div>
        
        <div class="success-banner mt-sm" *ngIf="success">
          Project imported successfully. Redirecting...
        </div>
        
        <div class="error-banner mt-sm" *ngIf="error">
          {{ error }}
          <button class="btn btn-sm btn-outline mt-sm" (click)="retry()" *ngIf="!uploading">Retry</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .projects-container { max-width: 600px; margin: 0 auto; }
    .card { background-color: var(--bg-panel); border: 1px solid var(--bg-panel-border); border-radius: var(--border-radius-lg); padding: 24px; }
    .form-group { display: flex; flex-direction: column; gap: 8px; }
    label { font-size: 14px; font-weight: 500; color: var(--text-primary); }
    .input { padding: 10px 12px; background: var(--bg-body); border: 1px solid var(--bg-panel-border); border-radius: var(--border-radius-md); color: var(--text-primary); }
    .input:focus { border-color: var(--accent-primary); outline: none; }
    .input:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn { padding: 8px 16px; border-radius: var(--border-radius-md); font-weight: 500; cursor: pointer; border: none; }
    .btn-sm { padding: 4px 10px; font-size: 12px; margin-top: 8px; display: block; }
    .btn-primary { background: linear-gradient(135deg, var(--accent-primary), #8b5cf6); color: white; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-outline { border: 1px solid var(--bg-panel-border); color: var(--text-primary); background: transparent; }
    .error-banner { background-color: rgba(248, 113, 113, 0.1); color: #f87171; padding: 12px; border-radius: 4px; border: 1px solid rgba(248, 113, 113, 0.2); }
    .success-banner { background-color: rgba(46, 160, 67, 0.1); color: var(--accent-success); padding: 12px; border-radius: 4px; border: 1px solid rgba(46, 160, 67, 0.2); font-weight: 500; }
  `]
})
export class ProjectImport {
  name = '';
  description = '';
  file: File | null = null;
  uploading = false;
  success = false;
  error = '';

  constructor(private projectService: ProjectService, private router: Router, private cdr: ChangeDetectorRef) {}

  onFileSelected(event: any) {
    this.file = event.target.files[0];
    this.error = '';
    if (this.file && !this.name) {
      this.name = this.file.name.replace('.zip', '');
    }
  }

  retry() {
    this.error = '';
    this.upload();
  }

  upload() {
    if (!this.file || !this.name) return;
    this.uploading = true;
    this.error = '';
    this.success = false;
    
    this.projectService.createProject(this.name, this.description, this.file).subscribe({
      next: (res) => {
        this.uploading = false;
        this.success = true;
        this.cdr.markForCheck();
        setTimeout(() => {
          this.router.navigate(['/projects', res.id]);
        }, 1000);
      },
      error: (err: HttpErrorResponse) => {
        this.uploading = false;
        this.success = false;
        
                if (err.error instanceof ErrorEvent) {
          this.error = `Network error: ${err.error.message}`;
        } else if (err.status === 0) {
          this.error = 'Unable to reach the backend server. Please verify that the backend is running on port 8081.';
        } else if (err.status === 413) {
          this.error = 'The ZIP file exceeds the maximum allowed size of 50MB.';
        } else if (err.status === 400) {
          this.error = 'The project import request is invalid.';
        } else if (err.status === 500 || err.status > 500) {
          this.error = 'The server encountered an error while importing the project.';
        } else if (err.error && typeof err.error === 'object') {
          this.error = `Project import failed: ${err.error.message || err.error.error || 'unexpected error occurred.'}`;
        } else if (typeof err.error === 'string') {
          this.error = `Project import failed: ${err.error}`;
        } else {
          this.error = `Project import failed: the server returned an unexpected error (Status: ${err.status}).`;
        }
        this.cdr.markForCheck();
      }
    });
  }
}
