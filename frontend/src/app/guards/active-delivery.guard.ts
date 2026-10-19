import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { OrderService } from '../services/order.service';
import { UserService } from '../services/user.service';
import { OrderStatus } from '../models/order-status.enum';

// HU07 (3ª revisão, decisão h): com uma entrega em rota, lista, histórico e o detalhe de outro
// pedido levam de volta ao detalhe dela; a saída é confirmar ou cancelar. O backend devolve a
// entrega em rota de despacho mais antigo primeiro. ADMIN não é preso.
export const activeDeliveryGuard: CanActivateFn = (route) => {
  const userService = inject(UserService);
  const orderService = inject(OrderService);
  const router = inject(Router);

  if (userService.getUserLocalStorage()?.userType !== 'DELIVERER') {
    return true;
  }

  return orderService.myRoutes().pipe(
    map((routes) => {
      const active = routes.find((order) => order.status === OrderStatus.OUT_FOR_DELIVERY);
      if (!active || route.paramMap.get('id') === active.id) {
        return true;
      }
      return router.createUrlTree(['/entregas', active.id]);
    }),
    // sem a lista não dá para saber se há entrega em rota; a própria tela mostra o erro ao carregar
    catchError(() => of(true))
  );
};
