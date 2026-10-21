import { HttpInterceptorFn } from '@angular/common/http';
import { AuthService } from '../services/auth.service';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  // HU13: token vencido não vai junto; a vitrine pública responde como para visitante
  const token = authService.isTokenExpired() ? null : authService.getToken();

  const authReq = token ? req.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`
            }
          }) : req;

  return next(authReq).pipe(
    catchError(err => {
      // só é sessão caída se a requisição levou token; senha errada ou visitante (HU13) não derrubam nada
      if (err.status === 401 && token) {
        authService.logout();
        const returnUrl = router.url;
        router.navigate(['/login'], returnUrl && !returnUrl.startsWith('/login') ? { queryParams: { returnUrl } } : {});
      }
      return throwError(() => err);
    })
  );
};
