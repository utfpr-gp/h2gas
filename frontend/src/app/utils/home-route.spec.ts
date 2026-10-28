import { homeFor } from './home-route';

describe('homeFor', () => {
  it('deveLevarCadaPerfilAoSeuInicio', () => {
    expect(homeFor('CUSTOMER')).toBe('/inicio');
    expect(homeFor('ADMIN')).toBe('/admin');
    expect(homeFor('SUPPLIER')).toBe('/minha-loja');
    expect(homeFor('DELIVERER')).toBe('/entregas');
    expect(homeFor(undefined)).toBe('/inicio');
  });
});
