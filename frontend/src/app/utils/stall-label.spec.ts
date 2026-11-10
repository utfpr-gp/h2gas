import { stallLabel } from './stall-label';

// HU28 Cenário 7: "parado há …" legível
describe('stallLabel', () => {
  it('deveUsarMinutosHorasEDias', () => {
    expect(stallLabel(20)).toBe('parado há 20 min');
    expect(stallLabel(70)).toBe('parado há 1 h');
    expect(stallLabel(130)).toBe('parado há 2 h');
    expect(stallLabel(1440)).toBe('parado há 1 dia');
    expect(stallLabel(6 * 1440)).toBe('parado há 6 dias');
  });
});
