
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const token = localStorage.getItem('stockflow_token');

  const isLoginRequest = req.url.endsWith('/api/auth/login');

  const authReq =
    token && req.url.startsWith('http://localhost:8080/api/')
      ? req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        })
      : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Le backend répond avec un ProblemDetail (champ « detail ») :
      // on l'expose aussi sous « message », lu par les écrans existants.
      if (
        error.error &&
        typeof error.error === 'object' &&
        typeof error.error.detail === 'string' &&
        !error.error.message
      ) {
        error.error.message = error.error.detail;
      }

      if (error.status === 401 && !isLoginRequest) {
        localStorage.removeItem('stockflow_token');
        localStorage.removeItem('stockflow_user');

        void router.navigate(['/login']);
      }

      return throwError(() => error);
    })
  );
};
