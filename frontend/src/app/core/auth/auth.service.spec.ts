import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  const response = { accessToken: 'jwt', tokenType: 'Bearer', expiresIn: 900, user: { id: 1, username: 'a', email: 'a@b.c', role: 'USER', enabled: true } };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  it('stores the session in memory after login', () => {
    service.login({ email: 'a@b.c', password: 'secret123' }).subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush(response);
    expect(service.isAuthenticated()).toBe(true);
    expect(service.accessToken).toBe('jwt');
    expect(localStorage.getItem('jwt')).toBeNull();
  });

  it('clears the session when refresh fails', () => {
    service.login({ email: 'a@b.c', password: 'secret123' }).subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush(response);
    service.refresh().subscribe();
    http.expectOne(`${environment.apiUrl}/auth/refresh`).flush({ message: 'x' }, { status: 401, statusText: 'Unauthorized' });
    expect(service.isAuthenticated()).toBe(false);
  });

  it('exposes admin role', () => {
    service.login({ email: 'a@b.c', password: 'secret123' }).subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush({ ...response, user: { ...response.user, role: 'ADMIN' } });
    expect(service.isAdmin()).toBe(true);
  });
});
