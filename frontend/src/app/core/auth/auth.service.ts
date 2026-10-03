import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/auth.model';

/**
 * Authentication state.
 * - The short-lived access token (JWT) is kept in memory only (not readable from localStorage -> not stealable by XSS persistence).
 * - The refresh token is an HttpOnly cookie managed by the backend; `restoreSession()` uses it after a page reload.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly base = `${environment.apiUrl}/auth`;

  private readonly tokenSignal = signal<string | null>(null);
  private readonly userSignal = signal<User | null>(null);
  private refreshInFlight$: Observable<AuthResponse | null> | null = null;

  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.tokenSignal() !== null && this.userSignal() !== null);
  readonly isAdmin = computed(() => this.userSignal()?.role === 'ADMIN');

  get accessToken(): string | null {
    return this.tokenSignal();
  }

  login(request: LoginRequest): Observable<User> {
    return this.http
      .post<AuthResponse>(`${this.base}/login`, request, { withCredentials: true })
      .pipe(tap((r) => this.setSession(r)), map((r) => r.user));
  }

  register(request: RegisterRequest): Observable<User> {
    return this.http
      .post<AuthResponse>(`${this.base}/register`, request, { withCredentials: true })
      .pipe(tap((r) => this.setSession(r)), map((r) => r.user));
  }

  /** Rotates the refresh cookie and obtains a new access token. Concurrent callers share one request. */
  refresh(): Observable<AuthResponse | null> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http.post<AuthResponse>(`${this.base}/refresh`, {}, { withCredentials: true }).pipe(
        tap((r) => this.setSession(r)),
        catchError(() => {
          this.clearSession();
          return of(null);
        }),
        finalize(() => (this.refreshInFlight$ = null)),
        shareReplay(1),
      );
    }
    return this.refreshInFlight$;
  }

  /** Called once at startup: if a refresh cookie exists, the user stays logged in after a reload. */
  restoreSession(): Observable<boolean> {
    return this.refresh().pipe(map((r) => r !== null));
  }

  me(): Observable<User> {
    return this.http.get<User>(`${this.base}/me`).pipe(tap((u) => this.userSignal.set(u)));
  }

  logout(redirect = true): void {
    this.http.post(`${this.base}/logout`, {}, { withCredentials: true }).pipe(catchError(() => of(null))).subscribe();
    this.clearSession();
    if (redirect) {
      void this.router.navigate(['/login']);
    }
  }

  /** Local logout without calling the API (used when the session is already known to be invalid). */
  expireSession(): void {
    this.clearSession();
    void this.router.navigate(['/login']);
  }

  private setSession(r: AuthResponse): void {
    this.tokenSignal.set(r.accessToken);
    this.userSignal.set(r.user);
  }

  private clearSession(): void {
    this.tokenSignal.set(null);
    this.userSignal.set(null);
  }
}
