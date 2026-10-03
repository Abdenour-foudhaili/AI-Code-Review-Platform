import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Diagram } from '../../core/models/analysis.model';
import { AnalysisService } from '../../core/services/analysis.service';
import { errorMessage } from '../../core/services/api-error';
import { DiagramViewer } from './diagram-viewer';

interface ArchGraph {
  nodes: { id: string; label: string; role: string; file: string; methods: number; complexity: number }[];
  roleCounts: Record<string, number>;
  styles: string[];
  technologies: string[];
  externalApis: string[];
}

@Component({
  selector: 'app-architecture',
  standalone: true,
  imports: [RouterLink, DiagramViewer],
  template: `
    <div class="flex-col gap-lg">
      <header class="flex justify-between items-center">
        <div><h1>Architecture</h1><p class="text-secondary">Structure detected by static analysis.</p></div>
        <a class="btn" [routerLink]="['/analyses', id]">← Back to report</a>
      </header>
      @if (error()) { <div class="error-banner" role="alert">{{ error() }}</div> }

      @if (graph(); as g) {
        <div class="card flex-col gap-sm">
          <h3>Overview</h3>
          @for (s of g.styles; track s) { <div>✓ {{ s }}</div> }
          <div>
            @for (r of roles(); track r[0]) { <span class="chip">{{ r[0] }}: {{ r[1] }}</span> }
          </div>
        </div>
      }

      @if (diagrams().length) {
        <div class="flex gap-sm" role="tablist">
          @for (d of diagrams(); track d.type; let i = $index) {
            <button class="btn" role="tab" [class.btn-primary]="i === active()" (click)="active.set(i)">{{ d.title }}</button>
          }
        </div>
        @if (current(); as d) {
          <app-diagram-viewer [source]="d.mermaid" [title]="d.title" />
          <details><summary class="text-secondary">Mermaid source</summary><pre class="card mono" style="overflow:auto;font-size:12px">{{ d.mermaid }}</pre></details>
        }
      }
    </div>
  `,
})
export class Architecture implements OnInit {
  private readonly api = inject(AnalysisService);
  private readonly route = inject(ActivatedRoute);
  readonly id = Number(this.route.snapshot.paramMap.get('id'));
  readonly diagrams = signal<Diagram[]>([]);
  readonly graph = signal<ArchGraph | null>(null);
  readonly active = signal(0);
  readonly error = signal('');

  current(): Diagram | null {
    return this.diagrams()[this.active()] ?? null;
  }

  roles(): [string, number][] {
    return Object.entries(this.graph()?.roleCounts ?? {});
  }

  ngOnInit(): void {
    this.api.diagrams(this.id).subscribe({ next: (r) => this.diagrams.set(r.diagrams), error: (e) => this.error.set(errorMessage(e)) });
    this.api.architecture(this.id).subscribe({ next: (g) => this.graph.set(g as ArchGraph), error: () => undefined });
  }
}
