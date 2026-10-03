import { Component, ElementRef, OnDestroy, effect, input, signal, viewChild } from '@angular/core';

/** Renders a Mermaid diagram with zoom (wheel / buttons), pan (drag) and export (SVG / PNG). Mermaid is loaded lazily. */
@Component({
  selector: 'app-diagram-viewer',
  standalone: true,
  template: `
    <div class="toolbar flex gap-sm items-center">
      <button class="btn btn-sm" (click)="zoom(1.2)" aria-label="Zoom in">＋</button>
      <button class="btn btn-sm" (click)="zoom(1 / 1.2)" aria-label="Zoom out">－</button>
      <button class="btn btn-sm" (click)="reset()">Reset</button>
      <span class="text-secondary" style="font-size:12px">{{ (scale() * 100).toFixed(0) }}%</span>
      <span style="flex:1"></span>
      <button class="btn btn-sm" (click)="exportSvg()" [disabled]="!svg()">Export SVG</button>
      <button class="btn btn-sm" (click)="exportPng()" [disabled]="!svg()">Export PNG</button>
    </div>
    @if (error()) { <div class="error-banner">{{ error() }}</div> }
    <div class="viewport" (wheel)="onWheel($event)" (mousedown)="startDrag($event)" (mousemove)="drag($event)" (mouseup)="endDrag()" (mouseleave)="endDrag()">
      <div #canvas class="canvas" [style.transform]="'translate(' + tx() + 'px,' + ty() + 'px) scale(' + scale() + ')'"></div>
    </div>
  `,
  styles: [`
    .toolbar { margin-bottom: 8px; }
    .viewport { height: 520px; overflow: hidden; border: 1px solid var(--bg-panel-border); border-radius: 8px; background: #fff; cursor: grab; position: relative; }
    .viewport:active { cursor: grabbing; }
    .canvas { transform-origin: 0 0; display: inline-block; padding: 16px; user-select: none; }
  `],
})
export class DiagramViewer implements OnDestroy {
  readonly source = input.required<string>();
  readonly title = input<string>('diagram');
  private readonly canvas = viewChild.required<ElementRef<HTMLDivElement>>('canvas');

  readonly scale = signal(1);
  readonly tx = signal(0);
  readonly ty = signal(0);
  readonly svg = signal('');
  readonly error = signal('');
  private dragging = false;
  private lastX = 0;
  private lastY = 0;
  private counter = 0;
  private destroyed = false;

  constructor() {
    effect(() => {
      const src = this.source();
      void this.render(src);
    });
  }

  ngOnDestroy(): void {
    this.destroyed = true;
  }

  private async render(src: string): Promise<void> {
    this.error.set('');
    try {
      const { default: mermaid } = await import('mermaid');
      mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', theme: 'default', flowchart: { htmlLabels: false } });
      const { svg } = await mermaid.render(`mmd-${Date.now()}-${this.counter++}`, src);
      if (this.destroyed) return;
      this.svg.set(svg);
      this.canvas().nativeElement.innerHTML = svg;   // strict security level: Mermaid sanitises the generated SVG
      this.reset();
    } catch (e) {
      this.svg.set('');
      this.canvas().nativeElement.innerHTML = '';
      this.error.set('The diagram could not be rendered.');
      console.error('Mermaid render error', e);
    }
  }

  zoom(factor: number): void {
    this.scale.update((s) => Math.min(4, Math.max(0.2, s * factor)));
  }

  reset(): void {
    this.scale.set(1);
    this.tx.set(0);
    this.ty.set(0);
  }

  onWheel(event: WheelEvent): void {
    event.preventDefault();
    this.zoom(event.deltaY < 0 ? 1.1 : 1 / 1.1);
  }

  startDrag(event: MouseEvent): void {
    this.dragging = true;
    this.lastX = event.clientX;
    this.lastY = event.clientY;
  }

  drag(event: MouseEvent): void {
    if (!this.dragging) return;
    this.tx.update((v) => v + event.clientX - this.lastX);
    this.ty.update((v) => v + event.clientY - this.lastY);
    this.lastX = event.clientX;
    this.lastY = event.clientY;
  }

  endDrag(): void {
    this.dragging = false;
  }

  exportSvg(): void {
    this.download(new Blob([this.svg()], { type: 'image/svg+xml' }), `${this.fileName()}.svg`);
  }

  exportPng(): void {
    const el = this.canvas().nativeElement.querySelector('svg');
    if (!el) return;
    const box = el.getBoundingClientRect();
    const width = Math.max(1, Math.ceil(box.width)) * 2;
    const height = Math.max(1, Math.ceil(box.height)) * 2;
    const clone = el.cloneNode(true) as SVGElement;
    clone.setAttribute('width', String(width));
    clone.setAttribute('height', String(height));
    const data = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(new XMLSerializer().serializeToString(clone));
    const img = new Image();
    img.onload = () => {
      const c = document.createElement('canvas');
      c.width = width;
      c.height = height;
      const ctx = c.getContext('2d');
      if (!ctx) return;
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, width, height);
      ctx.drawImage(img, 0, 0, width, height);
      c.toBlob((b) => b && this.download(b, `${this.fileName()}.png`));
    };
    img.src = data;
  }

  private fileName(): string {
    return this.title().toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'diagram';
  }

  private download(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
  }
}
