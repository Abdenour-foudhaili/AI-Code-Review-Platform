import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { adminGuard, authGuard } from './auth.guard';

describe('guards', () => {
  const authed = signal(false);
  const admin = signal(false);

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: { isAuthenticated: authed, isAdmin: admin } }],
    });
  });

  it('redirects anonymous users to /login with the return url', () => {
    authed.set(false);
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, { url: '/rules' } as never));
    expect(result instanceof UrlTree).toBe(true);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Frules');
  });

  it('lets authenticated users through', () => {
    authed.set(true);
    expect(TestBed.runInInjectionContext(() => authGuard({} as never, { url: '/rules' } as never))).toBe(true);
  });

  it('blocks non-admin users from admin routes', () => {
    admin.set(false);
    expect(TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never)) instanceof UrlTree).toBe(true);
  });
});
