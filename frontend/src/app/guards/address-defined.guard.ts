import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { catchError, map, of } from 'rxjs';
import { AddressService } from '../services/address.service';
import { UserService } from '../services/user.service';
import { LocationContextService } from '../services/location-context.service';
import { navigateKeepingReturnUrl } from '../utils/return-url';

export const addressDefinedGuard: CanActivateFn = (route, state) => {
  const locationContext = inject(LocationContextService);
  const addressService = inject(AddressService);
  const userService = inject(UserService);
  const router = inject(Router);
  const toastr = inject(ToastrService);

  if (locationContext.current()) {
    return true;
  }

  const user = userService.getUserLocalStorage();
  // HU13: o visitante escolhe a cidade pelo CEP antes de ver o catálogo (RN05)
  if (!user && route?.data?.['allowVisitor']) {
    navigateKeepingReturnUrl(router, '/onde-voce-esta', state?.url);
    return false;
  }
  // sem usuário numa rota que exige login, quem decide é o authGuard (que roda junto)
  if (!user) {
    return true;
  }
  // o admin não compra: vê o catálogo sem filtro de cidade e não cadastra endereço
  if (user?.userType === 'ADMIN') {
    return true;
  }

  return addressService.findByUserId(user?.id).pipe(
    map((response: any) => {
      locationContext.setAddress({
        cityId: response.cityId,
        cityName: response.cityName,
        label: `${response.streetName}, ${response.number} · ${response.cityName}`,
      });
      return true;
    }),
    catchError((err) => {
      if (err?.status === 404) {
        navigateKeepingReturnUrl(router, '/primeiro-acesso-endereco', state?.url); // HU13: do checkout, volta para ele
      } else {
        toastr.error('Não foi possível verificar seu endereço. Tente novamente em instantes.');
      }
      return of(false);
    })
  );
};
