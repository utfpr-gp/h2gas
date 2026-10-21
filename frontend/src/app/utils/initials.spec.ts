import { initials } from './initials';

describe('initials', () => {
  it('usa a primeira letra do primeiro e do último nome', () => {
    expect(initials('Maria Souza Lima')).toBe('ML');
    expect(initials('Igor Emanuel')).toBe('IE');
  });

  it('nome único usa as duas primeiras letras', () => {
    expect(initials('Igor')).toBe('IG');
    expect(initials('X')).toBe('X');
  });

  it('ignora espaços extras e usa maiúsculas', () => {
    expect(initials('  ana   maria  ')).toBe('AM');
  });

  it('devolve ? quando não há nome', () => {
    expect(initials('')).toBe('?');
    expect(initials(undefined)).toBe('?');
    expect(initials(null)).toBe('?');
  });
});
