import { City } from '../models/city';

export interface ViaCepCity {
  localidade?: string;
  uf?: string;
  ibge?: string;
}

export const normalizeName = (value?: string) =>
  (value ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').trim().toLowerCase();

// F7 fatia 6: casa o CEP com a cidade atendida pelo código IBGE; cidade antiga sem código casa por nome + UF
export function matchCityFromCep(cities: City[] | undefined, cep: ViaCepCity): City | undefined {
  const code = Number(cep.ibge);
  const byCode = code ? cities?.find((c) => c.ibgeCode === code) : undefined;
  if (byCode) return byCode;
  return cities?.find((c) => !c.ibgeCode
    && normalizeName(c.name) === normalizeName(cep.localidade)
    && normalizeName(c.state) === normalizeName(cep.uf));
}
