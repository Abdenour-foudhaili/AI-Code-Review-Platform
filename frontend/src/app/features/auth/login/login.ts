import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { errorMessage } from '../../../core/services/api-error';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  styleUrl: '../auth-page.css',
  template: `
    <div class="auth-wrapper">
      <form class="card auth-card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div class="brand"><span class="logo-icon">&lt;ai/&gt;</span><h2 style="margin:0">AI Code Review</h2></div>
        <p class="text-secondary">Sign in to analyse your repositories.</p>

        @if (error()) { <div class="error-banner" role="alert">{{ error() }}</div> }

        <div class="form-group">
          <label for="email">Email</label>
          <input id="email" type="email" formControlName="email" autocomplete="email" />
          @if (form.controls.email.touched && form.controls.email.invalid) { <span class="field-error">A valid email is required</span> }
        </div>
        <div class="form-group">
          <label for="password">Password</label>
          <input id="password" type="password" formControlName="password" autocomplete="current-password" />
          @if (form.controls.password.touched && form.controls.password.invalid) { <span class="field-error">Password is required</span> }
        </div>
        <button class="btn btn-primary" type="submit" [disabled]="loading()" style="justify-content:center">
          {{ loading() ? 'Signing in…' : 'Sign in' }}
        </button>
        <p class="alt">No account yet? <a routerLink="/register">Create one</a></p>
      </form>
    </div>
  `,
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        void this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : '/dashboard');
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(errorMessage(e, 'Login failed'));
      },
    });
  }
}
