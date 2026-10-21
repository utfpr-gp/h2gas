import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, shareReplay } from 'rxjs';

export const IBGE_URL = 'https://servicodados.ibge.gov.br/api/v1/localidades';

export interface IbgeState { code: number; uf: string; name: string; }
export interface IbgeMunicipality { code: number; name: string; }

// F7 fatia 6: UFs e municípios oficiais (API pública do IBGE, sem chave); guardados na sessão
@Injectable({ providedIn: 'root' })
export class IbgeService {
  private states$?: Observable<IbgeState[]>;
  private readonly municipalities$ = new Map<string, Observable<IbgeMunicipality[]>>();

  constructor(private http: HttpClient) {}

  states(): Observable<IbgeState[]> {
    this.states$ ??= this.http.get<{ id: number; sigla: string; nome: string }[]>(`${IBGE_URL}/estados?orderBy=nome`).pipe(
      map((list) => list.map((s) => ({ code: s.id, uf: s.sigla, name: s.nome }))),
      shareReplay(1),
    );
    return this.states$;
  }

  municipalities(uf: string): Observable<IbgeMunicipality[]> {
    let cached = this.municipalities$.get(uf);
    if (!cached) {
      cached = this.http.get<{ id: number; nome: string }[]>(`${IBGE_URL}/estados/${uf}/municipios?orderBy=nome`).pipe(
        map((list) => list.map((m) => ({ code: m.id, name: m.nome }))),
        shareReplay(1),
      );
      this.municipalities$.set(uf, cached);
    }
    return cached;
  }
}
