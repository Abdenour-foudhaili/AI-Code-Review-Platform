import { HttpErrorResponse } from '@angular/common/http';

/** Extracts a user-friendly message from the backend error shape {timestamp,status,error,message,path}. */
export function errorMessage(err: unknown, fallback = 'Something went wrong'): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) return 'Cannot reach the server';
    if (err.status === 403) return 'Access denied';
    const body = err.error;
    if (body && typeof body === 'object' && typeof body.message === 'string') return body.message;
    if (typeof body === 'string' && body.trim().length > 0 && body.length < 300) return body;
  }
  return fallback;
}
