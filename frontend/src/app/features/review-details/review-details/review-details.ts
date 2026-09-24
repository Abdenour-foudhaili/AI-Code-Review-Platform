import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ReviewService } from '../../../core/services/review.service';
import { ReviewDetailsResponse } from '../../../core/models/review.model';

@Component({
  selector: 'app-review-details',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="review-details-page flex-col gap-lg">
      <div *ngIf="loading" class="text-center py-4 text-secondary">Loading review details...</div>
      <div *ngIf="error" class="error-banner">{{ error }}</div>

      <ng-container *ngIf="!loading && review">
        <header class="page-header flex justify-between items-center">
          <div>
            <h1>{{ review.projectName }}</h1>
            <p class="text-secondary">Language: {{ review.language }} | Type: {{ review.reviewType }}</p>
          </div>
          <div class="actions flex gap-sm items-center">
            <span class="badge"
                  [class.badge-success]="review.status === 'COMPLETED'"
                  [class.badge-warning]="review.status === 'PENDING' || review.status === 'ANALYZING'"
                  [class.badge-danger]="review.status === 'FAILED'">
              {{ review.status }}
            </span>
            <button class="btn btn-primary" 
                    *ngIf="review.status === 'PENDING' || review.status === 'FAILED'" 
                    (click)="analyze()" 
                    [disabled]="analyzing">
              {{ analyzing ? 'Starting...' : 'Analyze Now' }}
            </button>
          </div>
        </header>

        <div *ngIf="review.status === 'ANALYZING'" class="analyzing-banner flex justify-between items-center">
          <div class="flex items-center gap-sm">
            <span class="spinner">o</span> AI is currently analyzing your code...
          </div>
          <button class="btn btn-outline btn-sm" (click)="loadReview()">Refresh</button>
        </div>

        <div class="grid layout-grid gap-lg" *ngIf="review.summary">
          
          <div class="col-summary">
            <div class="card flex-col gap-md">
              <h3>AI Summary</h3>
              <div class="score-box flex-col items-center">
                <div class="score-circle">{{ review.summary.qualityScore || 'N/A' }}</div>
                <div class="text-secondary text-sm">Quality Score</div>
              </div>
              <p class="summary-text">{{ review.summary.summary }}</p>
              
              <div class="stats-box flex-col gap-sm">
                <div class="stat-row flex justify-between">
                  <span class="text-secondary">Total Issues</span>
                  <span class="font-medium">{{ review.summary.totalIssues }}</span>
                </div>
                <div class="stat-row flex justify-between">
                  <span class="text-secondary">Critical</span>
                  <span class="text-critical font-medium">{{ review.summary.criticalIssues }}</span>
                </div>
                <div class="stat-row flex justify-between">
                  <span class="text-secondary">High</span>
                  <span class="text-high font-medium">{{ review.summary.highIssues }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="col-findings flex-col gap-md">
            <h3>Detected Findings ({{ review.findings?.length || 0 }})</h3>
            
            <div class="card p-0" *ngIf="!review.findings || review.findings.length === 0">
              <div class="p-4 text-center text-secondary">No findings detected.</div>
            </div>

            <div class="finding-card card flex-col gap-sm" *ngFor="let finding of review.findings; let i = index">
              <div class="finding-header flex justify-between items-center">
                <div class="flex items-center gap-sm">
                  <span class="badge"
                        [class.badge-danger]="finding.severity === 'CRITICAL' || finding.severity === 'HIGH'"
                        [class.badge-warning]="finding.severity === 'MEDIUM'"
                        [class.badge-success]="finding.severity === 'LOW' || finding.severity === 'INFO'">
                    {{ finding.severity }}
                  </span>
                  <span class="font-medium finding-title">{{ finding.title }}</span>
                </div>
                <span class="line-badge" *ngIf="finding.lineNumber">Line {{ finding.lineNumber }}</span>
              </div>
              
              <div class="finding-desc">
                <div class="text-secondary mb-xs font-medium">Problem:</div>
                <p>{{ finding.description }}</p>
              </div>
              
              <div class="recommendation-box">
                <div class="rec-title">Recommendation:</div>
                <div>{{ finding.recommendation }}</div>
              </div>
              
              <div class="fix-container mt-sm" *ngIf="finding.fixedCode">
                <button class="btn btn-sm btn-outline" (click)="toggleFix(i)">
                  [{{ showFixMap.get(i) ? 'Hide Fix' : 'View Fix' }}]
                </button>
                
                <div class="fix-diff mt-md flex-col gap-sm" *ngIf="showFixMap.get(i)">
                  <div class="diff-original" *ngIf="finding.lineNumber && review.sourceCode">
                    <div class="diff-header">Original Code</div>
                    <div class="code-block bg-danger-light">
                      <pre><code>{{ getOriginalCodeSnippet(review.sourceCode, finding.lineNumber) }}</code></pre>
                    </div>
                  </div>
                  
                  <div class="diff-arrow flex justify-center text-secondary">
                    ↓
                  </div>
                  
                  <div class="diff-fixed">
                    <div class="diff-header text-success">Fixed Code</div>
                    <div class="code-block bg-success-light">
                      <pre><code>{{ finding.fixedCode }}</code></pre>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

        </div>
      </ng-container>
    </div>
  `,
  styles: [`
    .badge { padding: 4px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }
    .badge-success { background: rgba(46, 160, 67, 0.15); color: var(--accent-success); }
    .badge-warning { background: rgba(210, 153, 34, 0.15); color: var(--accent-warning); }
    .badge-danger { background: rgba(248, 81, 73, 0.15); color: var(--accent-danger); }
    
    .btn { padding: 6px 16px; border-radius: var(--border-radius-md); font-weight: 500; cursor: pointer; border: none; }
    .btn-sm { padding: 4px 10px; font-size: 12px; }
    .btn-primary { background: linear-gradient(135deg, var(--accent-primary), #8b5cf6); color: white; }
    .btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-outline { border: 1px solid var(--bg-panel-border); color: var(--text-primary); background: transparent; }
    .btn-outline:hover { background: var(--bg-panel-hover); }

    .error-banner { background-color: rgba(248, 113, 113, 0.1); color: #f87171; padding: 1rem; border-radius: 4px; border: 1px solid rgba(248, 113, 113, 0.2); }
    .analyzing-banner { background-color: rgba(88, 166, 255, 0.1); padding: 1rem; border-radius: var(--border-radius-md); border: 1px solid var(--accent-primary); margin-bottom: 1rem; }
    .spinner { display: inline-block; animation: spin 1s linear infinite; }
    @keyframes spin { 100% { transform: rotate(360deg); } }

    .layout-grid { display: grid; grid-template-columns: 1fr 2fr; }
    .card { background-color: var(--bg-panel); border: 1px solid var(--bg-panel-border); border-radius: var(--border-radius-lg); padding: var(--spacing-lg); }
    
    .score-box { margin-bottom: 1rem; }
    .score-circle { width: 80px; height: 80px; border-radius: 50%; background: linear-gradient(135deg, var(--accent-primary), #8b5cf6); display: flex; align-items: center; justify-content: center; font-size: 32px; font-weight: bold; color: white; margin-bottom: 8px; }
    
    .summary-text { color: var(--text-secondary); line-height: 1.5; font-size: 14px; margin-bottom: 1rem; }
    .stats-box { background: var(--bg-body); padding: 16px; border-radius: var(--border-radius-sm); }
    .stat-row { padding-bottom: 8px; border-bottom: 1px solid var(--bg-panel-border); font-size: 14px; }
    .stat-row:last-child { border-bottom: none; padding-bottom: 0; }
    
    .text-critical { color: var(--accent-danger); }
    .text-high { color: #fb923c; }
    
    .finding-title { font-size: 16px; color: var(--text-primary); }
    .line-badge { font-family: 'Fira Code', monospace; color: var(--text-secondary); font-size: 13px; background: var(--bg-body); padding: 2px 8px; border-radius: 4px; border: 1px solid var(--bg-panel-border); }
    .finding-desc { color: var(--text-secondary); font-size: 14px; margin: 8px 0; }
    .recommendation-box { background: rgba(88, 166, 255, 0.05); border-left: 3px solid var(--accent-primary); padding: 12px; font-size: 14px; color: var(--text-primary); }
    .rec-title { font-weight: 600; color: var(--accent-primary); margin-bottom: 4px; font-size: 12px; text-transform: uppercase; }
    
    .code-block { background: #1e1e1e; padding: 12px; border-radius: 6px; overflow-x: auto; font-family: 'Fira Code', monospace; font-size: 13px; margin-top: 4px; border: 1px solid rgba(255,255,255,0.1); }
    .bg-danger-light { background: rgba(248, 81, 73, 0.05); border-color: rgba(248, 81, 73, 0.2); }
    .bg-success-light { background: rgba(46, 160, 67, 0.05); border-color: rgba(46, 160, 67, 0.2); }
    
    .diff-header { font-size: 12px; font-weight: 600; color: var(--text-secondary); text-transform: uppercase; margin-bottom: 4px; }
    .text-success { color: var(--accent-success); }
    .mb-xs { margin-bottom: 4px; }
    .mt-sm { margin-top: 8px; }
    .mt-md { margin-top: 16px; }
    
    .p-0 { padding: 0 !important; }
    .p-4 { padding: 16px; }
  `]
})
export class ReviewDetails implements OnInit {
  review: ReviewDetailsResponse | null = null;
  loading = true;
  error = '';
  analyzing = false;
  id!: number;
  showFixMap = new Map<number, boolean>();

  constructor(
    private route: ActivatedRoute, 
    private reviewService: ReviewService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    if (this.id) {
      this.loadReview();
    }
  }

  toggleFix(index: number) {
    this.showFixMap.set(index, !this.showFixMap.get(index));
    this.cdr.markForCheck();
  }

  getOriginalCodeSnippet(sourceCode: string, lineNumber: number): string {
    if (!sourceCode || !lineNumber) return '';
    const lines = sourceCode.split('\n');
    const idx = lineNumber - 1;
    if (idx >= 0 && idx < lines.length) {
      return lines[idx].trim();
    }
    return '';
  }

  loadReview() {
    this.loading = true;
    this.reviewService.getReviewById(this.id).subscribe({
      next: (data) => {
        this.review = data;
        this.loading = false;
        this.error = '';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = 'Failed to load review details.';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  analyze() {
    this.analyzing = true;
    this.reviewService.analyzeReview(this.id).subscribe({
      next: () => {
        this.analyzing = false;
        this.loadReview(); // refresh the view
      },
      error: (err) => {
        this.analyzing = false;
        alert(err.error?.message || 'Failed to start analysis.');
        this.cdr.markForCheck();
      }
    });
  }
}
