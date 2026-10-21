import { homeFor } from './home-route';

describe('homeFor', () => {
  it('deveLevarCadaPerfilAoSeuInicio', () => {
    expect(homeFor('CUSTOMER')).toBe('/inicio');
    expect(homeFor('ADMIN')).toBe('/inicio');
    expect(homeFor('SUPPLIER')).toBe('/dashboard');
    expect(homeFor('DELIVERER')).toBe('/entregas');
    expect(homeFor(undefined)).toBe('/inicio');
  });
});
