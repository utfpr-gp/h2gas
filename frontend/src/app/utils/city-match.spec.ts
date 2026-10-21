import { matchCityFromCep } from './city-match';
import { City } from '../models/city';

describe('matchCityFromCep', () => {
  const curitiba: City = { id: 1, name: 'Curitiba', state: 'PR', ibgeCode: 4106902 };
  const saoJoseAntiga: City = { id: 2, name: 'São José dos Pinhais', state: 'PR' };
  const bomJesusPi: City = { id: 3, name: 'Bom Jesus', state: 'PI' };
  const cities = [curitiba, saoJoseAntiga, bomJesusPi];

  it('deveCasarPeloCodigoIbge', () => {
    expect(matchCityFromCep(cities, { localidade: 'Curitiba (grafia diferente)', uf: 'PR', ibge: '4106902' })).toBe(curitiba);
  });

  it('deveCasarPorNomeSemAcentoEUf', () => {
    expect(matchCityFromCep(cities, { localidade: 'SAO JOSE DOS PINHAIS', uf: 'pr', ibge: '4125506' })).toBe(saoJoseAntiga);
  });

  it('naoDeveCasarMesmoNomeEmOutraUf', () => {
    expect(matchCityFromCep(cities, { localidade: 'Bom Jesus', uf: 'RS', ibge: '4302402' })).toBeUndefined();
  });

  it('naoDeveCasarPorNomeQuandoACidadeTemOutroCodigo', () => {
    expect(matchCityFromCep(cities, { localidade: 'Curitiba', uf: 'PR', ibge: '1234567' })).toBeUndefined();
  });
});
