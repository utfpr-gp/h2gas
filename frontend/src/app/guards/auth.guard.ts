import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserService } from '../services/user.service';

export const authGuard: CanActivateFn = (route, state) => {
  debugger
  const authService = inject(AuthService);
  const userService = inject(UserService)
  const router = inject(Router);
  const expectedRoles = route.data?.['roles'] as string[];

  const user = userService.getUserLocalStorage();

  if (!authService.isLogged()) {
    router.navigate(['/login']);
    return false;
  }

  if (expectedRoles && !expectedRoles.includes(user?.userType)) {
    router.navigate(['/unauthorized']); // ou qualquer rota de acesso negado
    return false;
  }

  return true;

};
