import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
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
          <input type="text" class="input" [(ngModel)]="name" placeholder="E.g. E-Commerce API">
        </div>
        <div class="form-group">
          <label>Description (Optional)</label>
          <input type="text" class="input" [(ngModel)]="description" placeholder="Project description">
        </div>
        <div class="form-group">
          <label>Project ZIP Archive</label>
          <input type="file" class="input" (change)="onFileSelected($event)" accept=".zip">
          <p class="text-secondary text-sm mt-xs">Select a valid ZIP file. Max size 50MB. Ignored dirs: node_modules, .git, etc.</p>
        </div>
        
        <div class="flex gap-sm mt-md">
          <button class="btn btn-primary" [disabled]="!file || !name || uploading" (click)="upload()">
            {{ uploading ? 'Uploading & Scanning...' : 'Import Project' }}
          </button>
          <button class="btn btn-outline" routerLink="/projects" [disabled]="uploading">Cancel</button>
        </div>
        
        <div class="error-banner mt-sm" *ngIf="error">{{ error }}</div>
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
    .btn { padding: 8px 16px; border-radius: var(--border-radius-md); font-weight: 500; cursor: pointer; border: none; }
    .btn-primary { background: linear-gradient(135deg, var(--accent-primary), #8b5cf6); color: white; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-outline { border: 1px solid var(--bg-panel-border); color: var(--text-primary); background: transparent; }
    .error-banner { background-color: rgba(248, 113, 113, 0.1); color: #f87171; padding: 12px; border-radius: 4px; border: 1px solid rgba(248, 113, 113, 0.2); }
  `]
})
export class ProjectImport {
  name = '';
  description = '';
  file: File | null = null;
  uploading = false;
  error = '';

  constructor(private projectService: ProjectService, private router: Router, private cdr: ChangeDetectorRef) {}

  onFileSelected(event: any) {
    this.file = event.target.files[0];
    if (this.file && !this.name) {
      this.name = this.file.name.replace('.zip', '');
    }
  }

  upload() {
    if (!this.file || !this.name) return;
    this.uploading = true;
    this.error = '';
    
    this.projectService.createProject(this.name, this.description, this.file).subscribe({
      next: (res) => {
        this.uploading = false;
        this.router.navigate(['/projects', res.id]);
      },
      error: (err) => {
        this.uploading = false;
        this.error = err.error || 'Failed to import project';
        this.cdr.markForCheck();
      }
    });
  }
}
