import { DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, interval, startWith, switchMap } from 'rxjs';
import { AnalysisDetails, AnalysisStatus, CodeContext, Finding } from '../../../core/models/analysis.model';
import { AnalysisService } from '../../../core/services/analysis.service';
import { errorMessage } from '../../../core/services/api-error';

const SEV_ORDER = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'];
const CATEGORIES = ['BUG', 'SECURITY', 'CODE_SMELL', 'ARCHITECTURE', 'PERFORMANCE', 'REFACTORING', 'TEST', 'BEST_PRACTICE', 'STYLE'];

@Component({
  selector: 'app-analysis-detail',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <div class="flex-col gap-lg">
      @if (error()) { <div class="error-banner" role="alert">{{ error() }}</div> }

      @if (status(); as s) {
        @if (s.status === 'ANALYZING') {
          <section class="card flex-col gap-md" aria-live="polite">
            <h2 style="margin:0">Analysis in progress…</h2>
            <div class="progress"><div class="bar" [style.width.%]="s.progress"></div></div>
            <ul class="stages">
              @for (st of s.stages; track st.key) {
                <li [class]="st.state"><span class="mark">{{ st.state === 'done' ? '✓' : st.state === 'active' ? '⏳' : '•' }}</span> {{ st.label }}</li>
              }
            </ul>
            <p class="text-secondary">{{ s.message }}</p>
          </section>
        } @else if (s.status === 'FAILED') {
          <div class="error-banner" role="alert"><strong>Analysis failed.</strong> {{ s.error || 'Unknown error' }}
            <div style="margin-top:8px"><a routerLink="/repositories" class="btn btn-sm">Back to repositories</a></div></div>
        }
      }

      @if (details(); as d) {
        <header class="flex justify-between items-center">
          <div>
            <h1 style="margin-bottom:4px">{{ d.summary.name }}</h1>
            <div class="text-secondary">
              Branch <strong>{{ d.summary.branch || '—' }}</strong> · Commit <span class="mono">{{ d.summary.commitHash ? d.summary.commitHash.substring(0, 7) : '—' }}</span>
              · {{ d.summary.createdAt | date: 'medium' }} · Status <span class="badge" [class.badge-success]="d.summary.status === 'COMPLETED'" [class.badge-warning]="d.summary.status === 'ANALYZING'" [class.badge-danger]="d.summary.status === 'FAILED'">{{ d.summary.status }}</span>
            </div>
          </div>
          <div class="flex gap-sm">
            @if (d.summary.status === 'COMPLETED') { <a class="btn" [routerLink]="['/architecture', d.summary.id]">Architecture &amp; diagrams</a> }
            <a class="btn" routerLink="/analyses">History</a>
          </div>
        </header>

        @if (d.summary.status === 'COMPLETED') {
          <section class="grid grid-cols-4 gap-md">
            <div class="card"><div class="text-secondary">Quality score</div><div class="big">{{ d.summary.qualityScore || '—' }}</div></div>
            <div class="card"><div class="text-secondary">Findings</div><div class="big">{{ d.summary.findingsCount }}</div></div>
            <div class="card"><div class="text-secondary">Critical + High</div><div class="big text-critical">{{ (d.bySeverity['CRITICAL'] || 0) + (d.bySeverity['HIGH'] || 0) }}</div></div>
            <div class="card"><div class="text-secondary">Security</div><div class="big">{{ d.byCategory['SECURITY'] || 0 }}</div></div>
          </section>

          <section class="card flex-col gap-sm">
            <h3>Summary</h3>
            <p style="white-space:pre-line">{{ d.summaryText }}</p>
            <div>@for (t of d.technologies; track t) { <span class="chip">{{ t }}</span> }</div>
            @for (w of d.warnings; track w) { <div class="warn-banner">⚠ {{ w }}</div> }
          </section>

          <section class="flex-col gap-md">
            <h2 style="margin:0">Findings</h2>
            <div class="filters flex gap-sm" style="flex-wrap:wrap">
              <select [ngModel]="fCategory()" (ngModelChange)="fCategory.set($event); load()" aria-label="Filter by type">
                <option value="">All types</option>
                @for (c of categories; track c) { <option [value]="c">{{ c }}</option> }
              </select>
              <select [ngModel]="fSeverity()" (ngModelChange)="fSeverity.set($event); load()" aria-label="Filter by severity">
                <option value="">All severities</option>
                @for (s of severities; track s) { <option [value]="s">{{ s }}</option> }
              </select>
              <select [ngModel]="fSource()" (ngModelChange)="fSource.set($event); load()" aria-label="Filter by source">
                <option value="">Static + AI</option><option value="STATIC">Static analysis</option><option value="LLM">AI</option>
              </select>
              <input type="search" placeholder="File contains…" [ngModel]="fFile()" (ngModelChange)="fFile.set($event)" (keyup.enter)="load()" (blur)="load()" />
            </div>

            <div class="table-container">
              <table class="data-table">
                <thead><tr><th>Severity</th><th>Type</th><th>Finding</th><th>File</th><th>Source</th></tr></thead>
                <tbody>
                  @for (f of sorted(); track f.id) {
                    <tr class="row" [class.selected]="selected()?.id === f.id" (click)="select(f)" tabindex="0" (keyup.enter)="select(f)">
                      <td><span class="badge sev-{{ f.severity }}">{{ f.severity }}</span></td>
                      <td><span class="badge badge-outline">{{ f.category }}</span></td>
                      <td><strong>{{ f.title }}</strong></td>
                      <td class="mono" style="font-size:12px">{{ f.file }}@if (f.lineNumber) {:{{ f.lineNumber }}}</td>
                      <td><span class="badge badge-outline">{{ f.source === 'LLM' ? 'AI' : 'Static' }}</span></td>
                    </tr>
                  } @empty {
                    <tr><td colspan="5" class="text-center text-secondary">No finding matches the filters.</td></tr>
                  }
                </tbody>
              </table>
            </div>
          </section>

          @if (selected(); as f) {
            <section class="card flex-col gap-md" aria-label="Finding detail">
              <div class="flex justify-between items-center">
                <h3 style="margin:0">{{ f.title }}</h3>
                <button class="btn btn-sm" (click)="selected.set(null)">Close</button>
              </div>
              <div class="text-secondary mono" style="font-size:12px">{{ f.file }}@if (f.lineNumber) {:{{ f.lineNumber }}} @if (f.ruleId) {· {{ f.ruleId }}}</div>
              <p style="white-space:pre-line">{{ f.description }}</p>
              @if (f.recommendation) { <div><strong>Recommendation</strong><p style="white-space:pre-line">{{ f.recommendation }}</p></div> }
              @if (f.fixedCode) { <div><strong>Suggested code</strong><pre class="code">{{ f.fixedCode }}</pre></div> }
              @if (codeError()) { <div class="warn-banner">{{ codeError() }}</div> }
              @if (code(); as c) {
                <div><strong>Code context</strong>
                  <pre class="code"><code>@for (l of c.lines; track $index) {<span class="ln" [class.hit]="c.startLine + $index === c.line"><span class="no">{{ c.startLine + $index }}</span>{{ l }}
</span>}</code></pre>
                </div>
              }
            </section>
          }
        }
      }
    </div>
  `,
  styles: [`
    .big { font-size: 32px; font-weight: 700; }
    .progress { height: 10px; background: var(--bg-main); border-radius: 6px; overflow: hidden; }
    .bar { height: 100%; background: var(--accent-primary); transition: width .4s; }
    .stages { list-style: none; padding: 0; margin: 0; display: grid; gap: 6px; }
    .stages li.done { color: var(--accent-success); } .stages li.active { color: var(--accent-primary); font-weight: 600; } .stages li.pending { color: var(--text-muted); }
    .mark { display: inline-block; width: 22px; }
    .row { cursor: pointer; } .row:hover td, .row.selected td { background: var(--bg-panel-hover); }
    .code { background: var(--bg-main); border: 1px solid var(--bg-panel-border); border-radius: 6px; padding: 12px; overflow: auto; font-family: 'Fira Code', monospace; font-size: 12px; white-space: pre; margin-top: 6px; }
    .ln { display: block; } .ln.hit { background: rgba(248,81,73,.18); } .no { display: inline-block; width: 44px; color: var(--text-muted); user-select: none; }
  `],
})
export class AnalysisDetail implements OnInit, OnDestroy {
  private readonly api = inject(AnalysisService);
  private readonly route = inject(ActivatedRoute);
  private poll?: Subscription;

  readonly id = Number(this.route.snapshot.paramMap.get('id'));
  readonly categories = CATEGORIES;
  readonly severities = SEV_ORDER;

  readonly status = signal<AnalysisStatus | null>(null);
  readonly details = signal<AnalysisDetails | null>(null);
  readonly findings = signal<Finding[]>([]);
  readonly selected = signal<Finding | null>(null);
  readonly code = signal<CodeContext | null>(null);
  readonly codeError = signal('');
  readonly error = signal('');
  readonly fCategory = signal('');
  readonly fSeverity = signal('');
  readonly fSource = signal('');
  readonly fFile = signal('');

  readonly sorted = computed(() =>
    [...this.findings()].sort((a, b) => SEV_ORDER.indexOf(a.severity) - SEV_ORDER.indexOf(b.severity) || (a.file ?? '').localeCompare(b.file ?? '')),
  );

  ngOnInit(): void {
    this.poll = interval(2000)
      .pipe(startWith(0), switchMap(() => this.api.status(this.id)))
      .subscribe({
        next: (s) => {
          const wasRunning = this.status()?.status === 'ANALYZING' || this.status() === null;
          this.status.set(s);
          if (s.status !== 'ANALYZING') {
            this.poll?.unsubscribe();
            if (wasRunning || !this.details()) this.loadReport();
          } else if (!this.details()) {
            this.api.details(this.id).subscribe({ next: (d) => this.details.set(d), error: () => undefined });
          }
        },
        error: (e) => {
          this.error.set(errorMessage(e, 'Could not load the analysis'));
          this.poll?.unsubscribe();
        },
      });
  }

  ngOnDestroy(): void {
    this.poll?.unsubscribe();
  }

  private loadReport(): void {
    this.api.details(this.id).subscribe({ next: (d) => this.details.set(d), error: (e) => this.error.set(errorMessage(e)) });
    this.load();
  }

  load(): void {
    this.api
      .findings(this.id, { category: this.fCategory(), severity: this.fSeverity(), source: this.fSource(), file: this.fFile() })
      .subscribe({ next: (f) => this.findings.set(f), error: (e) => this.error.set(errorMessage(e)) });
  }

  select(f: Finding): void {
    this.selected.set(f);
    this.code.set(null);
    this.codeError.set('');
    if (f.file && f.lineNumber && f.file !== '(repository)') {
      this.api.code(this.id, f.file, f.lineNumber).subscribe({
        next: (c) => this.code.set(c),
        error: () => this.codeError.set('The source of this file is not stored with the analysis.'),
      });
    }
  }
}
