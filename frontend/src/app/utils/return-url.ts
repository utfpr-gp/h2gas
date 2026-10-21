import { Router } from '@angular/router';
import { homeFor } from './home-route';

// HU13: depois de entrar, o cliente volta para onde estava — só caminho interno do app (nada de outro site nem do próprio login)
export function safeReturnUrl(raw: string | null | undefined): string | null {
  if (!raw || !raw.startsWith('/') || raw.startsWith('//') || raw.startsWith('/\\')) {
    return null;
  }
  if (/^\/(login|register)(\/|\?|$)/.test(raw)) {
    return null;
  }
  return raw;
}

export const isCheckoutReturn = (raw: string | null | undefined) => !!safeReturnUrl(raw)?.startsWith('/resumo-pedido');

// leva o retorno adiante (login ↔ cadastro, guardas); sem retorno, navega como antes
export function navigateKeepingReturnUrl(router: Router, path: string, raw: string | null | undefined) {
  const returnUrl = safeReturnUrl(raw);
  return returnUrl ? router.navigate([path], { queryParams: { returnUrl } }) : router.navigate([path]);
}

export function navigateAfterLogin(router: Router, raw: string | null | undefined, userType?: string) {
  const returnUrl = safeReturnUrl(raw);
  return returnUrl ? router.navigateByUrl(returnUrl) : router.navigate([homeFor(userType)]);
}
