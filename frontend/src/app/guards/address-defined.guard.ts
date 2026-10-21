import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { catchError, map, of } from 'rxjs';
import { AddressService } from '../services/address.service';
import { UserService } from '../services/user.service';
import { LocationContextService } from '../services/location-context.service';

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
        router.navigate(['/primeiro-acesso-endereco']);
      } else {
        toastr.error('Não foi possível verificar seu endereço. Tente novamente em instantes.');
      }
      return of(false);
    })
  );
};
