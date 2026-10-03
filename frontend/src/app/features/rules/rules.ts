import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ReviewRule } from '../../core/models/analysis.model';
import { errorMessage } from '../../core/services/api-error';
import { RulesService } from '../../core/services/rules.service';

@Component({
  selector: 'app-rules',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="flex-col gap-lg">
      <header><h1>Custom rules</h1><p class="text-secondary">Rules in plain language that the AI checks during every analysis.</p></header>
      @if (error()) { <div class="error-banner" role="alert">{{ error() }}</div> }

      <form class="card flex-col gap-md" (ngSubmit)="save()">
        <h3 style="margin:0">{{ editingId() ? 'Edit rule' : 'New rule' }}</h3>
        <div class="form-group"><label for="name">Name</label><input id="name" name="name" [(ngModel)]="name" maxlength="120" required /></div>
        <div class="form-group"><label for="ruleText">Rule</label>
          <textarea id="ruleText" name="ruleText" rows="3" [(ngModel)]="ruleText" maxlength="2000" required placeholder="e.g. Do not use System.out.println"></textarea></div>
        <div class="form-group"><label for="desc">Description (optional)</label><input id="desc" name="desc" [(ngModel)]="description" maxlength="500" /></div>
        <label class="flex items-center gap-sm"><input type="checkbox" name="enabled" [(ngModel)]="enabled" /> Enabled</label>
        <div class="flex gap-sm">
          <button class="btn btn-primary" type="submit" [disabled]="!name.trim() || !ruleText.trim()">{{ editingId() ? 'Update' : 'Add rule' }}</button>
          @if (editingId()) { <button class="btn" type="button" (click)="reset()">Cancel</button> }
        </div>
      </form>

      <div class="table-container">
        <table class="data-table">
          <thead><tr><th>Name</th><th>Rule</th><th>Enabled</th><th></th></tr></thead>
          <tbody>
            @for (r of rules(); track r.id) {
              <tr>
                <td><strong>{{ r.name }}</strong><div class="text-secondary" style="font-size:12px">{{ r.description }}</div></td>
                <td>{{ r.ruleText }}</td>
                <td><input type="checkbox" [checked]="r.enabled" (change)="toggle(r)" [attr.aria-label]="'Toggle ' + r.name" /></td>
                <td class="flex gap-sm"><button class="btn btn-sm" (click)="edit(r)">Edit</button><button class="btn btn-danger btn-sm" (click)="remove(r)">Delete</button></td>
              </tr>
            } @empty { <tr><td colspan="4" class="text-center text-secondary">No custom rule yet.</td></tr> }
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class Rules implements OnInit {
  private readonly api = inject(RulesService);
  readonly rules = signal<ReviewRule[]>([]);
  readonly error = signal('');
  readonly editingId = signal<number | null>(null);
  name = '';
  ruleText = '';
  description = '';
  enabled = true;

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.api.list().subscribe({ next: (r) => this.rules.set(r), error: (e) => this.error.set(errorMessage(e)) });
  }

  private payload() {
    return { name: this.name.trim(), ruleText: this.ruleText.trim(), description: this.description.trim(), enabled: this.enabled };
  }

  save(): void {
    const id = this.editingId();
    const call = id ? this.api.update(id, this.payload()) : this.api.create(this.payload());
    call.subscribe({
      next: () => {
        this.reset();
        this.reload();
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  edit(r: ReviewRule): void {
    this.editingId.set(r.id);
    this.name = r.name;
    this.ruleText = r.ruleText;
    this.description = r.description ?? '';
    this.enabled = r.enabled;
  }

  toggle(r: ReviewRule): void {
    this.api.update(r.id, { name: r.name, ruleText: r.ruleText, description: r.description ?? '', enabled: !r.enabled }).subscribe({
      next: () => this.reload(),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  remove(r: ReviewRule): void {
    if (!confirm(`Delete the rule "${r.name}"?`)) return;
    this.api.delete(r.id).subscribe({ next: () => this.reload(), error: (e) => this.error.set(errorMessage(e)) });
  }

  reset(): void {
    this.editingId.set(null);
    this.name = '';
    this.ruleText = '';
    this.description = '';
    this.enabled = true;
  }
}
