import { HttpInterceptorFn } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  // if (token) {
  //   const authReq = req.clone({
  //     setHeaders: {
  //       Authorization: `Bearer ${token}`
  //     }
  //   });
  //   return next(authReq);
  // }
  const authReq = token ? req.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`
            }
          }) : req;

  return next(authReq).pipe(
    catchError(err => {
      if (err.status === 401) {
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => err);
    })
  );
};
