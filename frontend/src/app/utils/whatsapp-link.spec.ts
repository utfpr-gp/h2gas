import { firstName, orderCode, whatsappLink } from './whatsapp-link';

// HU25
describe('whatsappLink', () => {
  it('deveMontarOLinkComDdiEMensagem', () => {
    expect(whatsappLink('(42) 98888-7777', 'Olá, Gás Centro! Pedido #4a1a4ffe'))
      .toBe('https://wa.me/5542988887777?text=Ol%C3%A1%2C%20G%C3%A1s%20Centro!%20Pedido%20%234a1a4ffe');
  });

  it('naoDeveRepetirO55', () => {
    expect(whatsappLink('+55 41 99999-0000', 'Oi')).toBe('https://wa.me/5541999990000?text=Oi');
  });

  it('deveDevolverNuloComTelefoneInvalido', () => {
    expect(whatsappLink(undefined, 'Oi')).toBeNull();
    expect(whatsappLink('', 'Oi')).toBeNull();
    expect(whatsappLink('9999-000', 'Oi')).toBeNull();
  });

  it('deveDarOCodigoEOPrimeiroNome', () => {
    expect(orderCode('4a1a4ffe-29b9-473a')).toBe('4a1a4ffe');
    expect(firstName('  Maria Souza ')).toBe('Maria');
    expect(firstName(undefined)).toBe('');
  });
});
