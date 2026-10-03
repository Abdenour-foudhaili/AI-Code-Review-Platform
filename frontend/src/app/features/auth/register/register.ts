import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { errorMessage } from '../../../core/services/api-error';

function matchPasswords(group: AbstractControl): ValidationErrors | null {
  const p = group.get('password')?.value;
  const c = group.get('confirm')?.value;
  return p && c && p !== c ? { mismatch: true } : null;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  styleUrl: '../auth-page.css',
  template: `
    <div class="auth-wrapper">
      <form class="card auth-card" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div class="brand"><span class="logo-icon">&lt;ai/&gt;</span><h2 style="margin:0">Create your account</h2></div>

        @if (error()) { <div class="error-banner" role="alert">{{ error() }}</div> }

        <div class="form-group">
          <label for="username">Username</label>
          <input id="username" formControlName="username" autocomplete="username" />
          @if (form.controls.username.touched && form.controls.username.invalid) { <span class="field-error">3-50 characters: letters, digits, . _ -</span> }
        </div>
        <div class="form-group">
          <label for="email">Email</label>
          <input id="email" type="email" formControlName="email" autocomplete="email" />
          @if (form.controls.email.touched && form.controls.email.invalid) { <span class="field-error">A valid email is required</span> }
        </div>
        <div class="form-group">
          <label for="password">Password</label>
          <input id="password" type="password" formControlName="password" autocomplete="new-password" />
          @if (form.controls.password.touched && form.controls.password.invalid) { <span class="field-error">8 to 72 characters</span> }
        </div>
        <div class="form-group">
          <label for="confirm">Confirm password</label>
          <input id="confirm" type="password" formControlName="confirm" autocomplete="new-password" />
          @if (form.touched && form.hasError('mismatch')) { <span class="field-error">Passwords do not match</span> }
        </div>
        <button class="btn btn-primary" type="submit" [disabled]="loading()" style="justify-content:center">
          {{ loading() ? 'Creating…' : 'Create account' }}
        </button>
        <p class="alt">Already registered? <a routerLink="/login">Sign in</a></p>
      </form>
    </div>
  `,
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group(
    {
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50), Validators.pattern(/^[A-Za-z0-9_.-]+$/)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirm: ['', [Validators.required]],
    },
    { validators: matchPasswords },
  );

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { username, email, password } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set('');
    this.auth.register({ username, email, password }).subscribe({
      next: () => void this.router.navigateByUrl('/dashboard'),
      error: (e) => {
        this.loading.set(false);
        this.error.set(errorMessage(e, 'Registration failed'));
      },
    });
  }
}
