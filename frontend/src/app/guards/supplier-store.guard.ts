import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserService } from '../services/user.service';

// F7 fatia 6: para o fornecedor, conta e loja são o mesmo cadastro — o "Meus Dados" dele é a "Minha loja"
export const supplierStoreGuard: CanActivateFn = () => {
  const user = inject(UserService).getUserLocalStorage();
  return user?.userType === 'SUPPLIER' ? inject(Router).createUrlTree(['/minha-loja']) : true;
};
