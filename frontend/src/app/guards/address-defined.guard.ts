import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AddressService } from '../services/address.service';
import { UserService } from '../services/user.service';
import { LocationContextService } from '../services/location-context.service';

export const addressDefinedGuard: CanActivateFn = (route, state) => {
  const locationContext = inject(LocationContextService);
  const addressService = inject(AddressService);
  const userService = inject(UserService);
  const router = inject(Router);

  if (locationContext.current()) {
    return true;
  }

  const user = userService.getUserLocalStorage();

  return addressService.findByUserId(user?.id).pipe(
    map((response: any) => {
      locationContext.setAddress({
        cityId: response.cityId,
        cityName: response.cityName,
        label: `${response.streetName}, ${response.number} · ${response.cityName}`,
      });
      return true;
    }),
    catchError(() => {
      router.navigate(['/primeiro-acesso-endereco']);
      return of(false);
    })
  );
};
