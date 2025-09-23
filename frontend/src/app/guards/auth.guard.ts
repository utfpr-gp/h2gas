import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserService } from '../services/user.service';
import { catchError, map, of, tap } from 'rxjs';
import { SupplierService } from '../services/supplier.service';

export const authGuard: CanActivateFn = (route, state) => {
  debugger
  const authService = inject(AuthService);
  const userService = inject(UserService)
  const router = inject(Router);
  const expectedRoles = route.data?.['roles'] as string[];
  const supplierService = inject(SupplierService);

  const user = userService.getUserLocalStorage();

  if (!authService.isLogged()) {
    router.navigate(['/login']);
    return false;
  }

  if (expectedRoles && !expectedRoles.includes(user?.userType)) {
    router.navigate(['/unauthorized']);
    return false;
  }
  debugger
  const skipSupplierCheckRoutes = ['/aguardando-aprovacao', '/login', '/register'];

  const currentUrl = state.url; // pega a rota que o usuário está tentando acessar

  if (user?.userType === 'SUPPLIER' && user?.supplierId) {
    if (skipSupplierCheckRoutes.includes(state.url)) {
      return true; 
    }

    return supplierService.getActiveSupplier(user.supplierId).pipe(
      tap((active: boolean) => {
        if (!active) {
          router.navigate(['/aguardando-aprovacao']);
        }
      }),
      map((active: boolean) => active),
      catchError(() => {
        router.navigate(['/login']);
        return of(false);
      })
    );
  }

  return true;

};
