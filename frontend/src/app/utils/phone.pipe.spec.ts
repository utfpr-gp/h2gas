import { PhonePipe } from './phone.pipe';

// HU28 Cenário 5: telefone legível; celular com 9 dígitos, fixo com 8
describe('PhonePipe', () => {
  const pipe = new PhonePipe();

  it('deveFormatarCelular', () => {
    expect(pipe.transform('42990000011')).toBe('(42) 99000-0011');
  });

  it('deveFormatarFixo', () => {
    expect(pipe.transform('4236220000')).toBe('(42) 3622-0000');
  });

  it('deveIgnorarODdiDoBrasilEOsSimbolos', () => {
    expect(pipe.transform('+55 (42) 99000-0011')).toBe('(42) 99000-0011');
    expect(pipe.transform('5542990000011')).toBe('(42) 99000-0011');
  });

  it('deveDevolverComoVeioQuandoNaoReconhece', () => {
    expect(pipe.transform('12345')).toBe('12345');
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });
});
