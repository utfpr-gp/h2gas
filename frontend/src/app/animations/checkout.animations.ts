import { animate, query, stagger, style, transition, trigger } from '@angular/animations';

const EASE_OUT = '250ms cubic-bezier(.2,.8,.2,1)';

// Etapa entra pela direita ao avançar e pela esquerda ao voltar
export const stepSlide = trigger('stepSlide', [
  transition(':increment', [
    style({ opacity: 0, transform: 'translateX(24px)' }),
    animate(EASE_OUT, style({ opacity: 1, transform: 'none' })),
  ]),
  transition(':decrement', [
    style({ opacity: 0, transform: 'translateX(-24px)' }),
    animate(EASE_OUT, style({ opacity: 1, transform: 'none' })),
  ]),
]);

// Item entra com fade/slide curto e, ao sair, colapsa a altura para a lista não "pular"
export const listItem = trigger('listItem', [
  transition(':enter', [
    style({ opacity: 0, transform: 'translateY(8px)' }),
    animate(EASE_OUT, style({ opacity: 1, transform: 'none' })),
  ]),
  transition(':leave', [
    style({ overflow: 'hidden' }),
    animate('200ms ease-in', style({ opacity: 0, transform: 'translateX(-16px)' })),
    animate('150ms ease-in', style({ height: 0, paddingTop: 0, paddingBottom: 0, marginTop: 0, marginBottom: 0 })),
  ]),
]);

export const fadeIn = trigger('fadeIn', [
  transition(':enter', [
    style({ opacity: 0, transform: 'scale(.96)' }),
    animate(EASE_OUT, style({ opacity: 1, transform: 'none' })),
  ]),
]);

// Ícone de sucesso "estoura" levemente ao aparecer
export const popIn = trigger('popIn', [
  transition(':enter', [
    style({ opacity: 0, transform: 'scale(.4)' }),
    animate('350ms cubic-bezier(.34,1.56,.64,1)', style({ opacity: 1, transform: 'none' })),
  ]),
]);

export const expandCollapse = trigger('expandCollapse', [
  transition(':enter', [
    style({ height: 0, opacity: 0, overflow: 'hidden' }),
    animate(EASE_OUT, style({ height: '*', opacity: 1 })),
  ]),
  transition(':leave', [
    style({ overflow: 'hidden' }),
    animate('200ms ease-in', style({ height: 0, opacity: 0 })),
  ]),
]);

// Cards de uma lista entram em sequência
export const staggerList = trigger('staggerList', [
  transition('* => *', [
    query(':enter', [
      style({ opacity: 0, transform: 'translateY(12px)' }),
      stagger(40, animate(EASE_OUT, style({ opacity: 1, transform: 'none' }))),
    ], { optional: true }),
  ]),
]);
