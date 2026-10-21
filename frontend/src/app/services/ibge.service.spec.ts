import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { IbgeService, IBGE_URL } from './ibge.service';

describe('IbgeService', () => {
  let service: IbgeService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(IbgeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deveListarUfsPorNome', () => {
    let ufs: any[] = [];
    service.states().subscribe((r) => (ufs = r));

    httpMock.expectOne(`${IBGE_URL}/estados?orderBy=nome`).flush([
      { id: 12, sigla: 'AC', nome: 'Acre' }, { id: 41, sigla: 'PR', nome: 'Paraná' },
    ]);

    expect(ufs).toEqual([{ code: 12, uf: 'AC', name: 'Acre' }, { code: 41, uf: 'PR', name: 'Paraná' }]);
  });

  it('deveListarMunicipiosDaUfComCache', () => {
    let primeira: any[] = [];
    let segunda: any[] = [];
    service.municipalities('PR').subscribe((r) => (primeira = r));
    httpMock.expectOne(`${IBGE_URL}/estados/PR/municipios?orderBy=nome`).flush([
      { id: 4100103, nome: 'Abatiá' }, { id: 4106902, nome: 'Curitiba' },
    ]);

    service.municipalities('PR').subscribe((r) => (segunda = r));
    httpMock.expectNone(`${IBGE_URL}/estados/PR/municipios?orderBy=nome`);

    expect(primeira).toEqual([{ code: 4100103, name: 'Abatiá' }, { code: 4106902, name: 'Curitiba' }]);
    expect(segunda).toEqual(primeira);
  });
});
