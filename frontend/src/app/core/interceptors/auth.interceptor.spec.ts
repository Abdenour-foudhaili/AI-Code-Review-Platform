import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()] });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  it('adds the bearer token to API calls only', () => {
    auth.login({ email: 'a@b.c', password: 'x' }).subscribe();
    ctrl.expectOne(`${environment.apiUrl}/auth/login`).flush({ accessToken: 'tok', tokenType: 'Bearer', expiresIn: 1, user: { id: 1, username: 'a', email: 'a@b.c', role: 'USER', enabled: true } });
    http.get(`${environment.apiUrl}/dashboard`).subscribe();
    expect(ctrl.expectOne(`${environment.apiUrl}/dashboard`).request.headers.get('Authorization')).toBe('Bearer tok');
    http.get('https://example.org/x').subscribe();
    expect(ctrl.expectOne('https://example.org/x').request.headers.has('Authorization')).toBe(false);
  });

  it('refreshes once on 401 and replays the request', () => {
    let result: unknown;
    http.get(`${environment.apiUrl}/dashboard`).subscribe((r) => (result = r));
    ctrl.expectOne(`${environment.apiUrl}/dashboard`).flush({}, { status: 401, statusText: 'Unauthorized' });
    ctrl.expectOne(`${environment.apiUrl}/auth/refresh`).flush({ accessToken: 'new', tokenType: 'Bearer', expiresIn: 1, user: { id: 1, username: 'a', email: 'a@b.c', role: 'USER', enabled: true } });
    const replay = ctrl.expectOne(`${environment.apiUrl}/dashboard`);
    expect(replay.request.headers.get('Authorization')).toBe('Bearer new');
    replay.flush({ ok: true });
    expect(result).toEqual({ ok: true });
  });
});
