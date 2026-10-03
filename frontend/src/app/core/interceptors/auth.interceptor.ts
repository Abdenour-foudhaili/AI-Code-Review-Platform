import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

const isApi = (url: string) => url.startsWith(environment.apiUrl);
const isAuthEndpoint = (url: string) => /\/auth\/(login|register|refresh|logout)$/.test(url);

function withToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}

/**
 * - Adds the JWT to every API call.
 * - On 401 (expired access token) refreshes once and replays the request; if that fails, logs out and goes to /login.
 * - 403 is passed through to the caller (the UI shows "access denied").
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  if (!isApi(req.url)) {
    return next(req);
  }
  return next(withToken(req, auth.accessToken)).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isAuthEndpoint(req.url)) {
        return auth.refresh().pipe(
          switchMap((session) => {
            if (!session) {
              auth.expireSession();
              return throwError(() => error);
            }
            return next(withToken(req, session.accessToken));
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};
