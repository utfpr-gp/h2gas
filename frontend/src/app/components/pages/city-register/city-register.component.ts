import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { CityService } from '../../../services/city.service';
import { IbgeMunicipality, IbgeService, IbgeState } from '../../../services/ibge.service';
import { City } from '../../../models/city';
import { backendMessage } from '../../../utils/backend-message';
import { normalizeName } from '../../../utils/city-match';
import { fadeIn } from '../../../animations/checkout.animations';

// F7 fatia 6: cidades atendidas, cadastradas pela base oficial do IBGE (UF → município)
@Component({
  selector: 'app-city-register',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './city-register.component.html',
  host: { class: 'flex-1 flex flex-col' },
  animations: [fadeIn],
})
export class CityRegisterComponent implements OnInit {
  cities: City[] = [];
  loading = true;
  query = '';

  creating = false;
  states: IbgeState[] = [];
  uf = '';
  municipalities: IbgeMunicipality[] = [];
  loadingMunicipalities = false;
  ibgeCode?: number;
  saving = false;

  deleteTarget?: City;
  deleting = false;

  constructor(
    private cityService: CityService,
    private ibgeService: IbgeService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    this.cityService.findAll().subscribe({
      next: (cities) => {
        this.cities = cities;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao carregar as cidades: ${backendMessage(err)}`);
      },
    });
  }

  // lista curta e só do admin: a busca filtra o que já veio
  get filtered(): City[] {
    const q = normalizeName(this.query);
    if (!q) return this.cities;
    return this.cities.filter((c) => normalizeName(`${c.name} ${c.state}`).includes(q));
  }

  onSearch(value: string) {
    this.query = value;
  }

  // ---- nova cidade ----
  openCreate() {
    this.creating = true;
    this.uf = '';
    this.municipalities = [];
    this.ibgeCode = undefined;
    if (!this.states.length) {
      this.ibgeService.states().subscribe({
        next: (states) => (this.states = states),
        error: (err) => this.toastr.error(`Não foi possível carregar os estados do IBGE: ${backendMessage(err)}`),
      });
    }
  }

  closeCreate() {
    if (!this.saving) this.creating = false;
  }

  selectUf(uf: string) {
    this.uf = uf;
    this.ibgeCode = undefined;
    this.municipalities = [];
    if (!uf) return;
    this.loadingMunicipalities = true;
    this.ibgeService.municipalities(uf).subscribe({
      next: (list) => {
        this.municipalities = list;
        this.loadingMunicipalities = false;
      },
      error: (err) => {
        this.loadingMunicipalities = false;
        this.toastr.error(`Não foi possível carregar as cidades do IBGE: ${backendMessage(err)}`);
      },
    });
  }

  selectMunicipality(code: string) {
    this.ibgeCode = code ? Number(code) : undefined;
  }

  // cidade antiga (sem código) conta como atendida pelo nome + UF
  isServed(m: IbgeMunicipality): boolean {
    return this.cities.some((c) => c.ibgeCode === m.code
      || (!c.ibgeCode && c.state === this.uf && normalizeName(c.name) === normalizeName(m.name)));
  }

  saveCreate() {
    if (!this.ibgeCode) return;
    this.saving = true;
    this.cityService.createFromIbge(this.ibgeCode).subscribe({
      next: (city) => {
        this.saving = false;
        this.creating = false;
        this.toastr.success(`${city.name} - ${city.state} agora é atendida.`);
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.toastr.error(`Não foi possível cadastrar a cidade: ${backendMessage(err)}`);
      },
    });
  }

  // ---- exclusão ----
  askDelete(city: City) {
    this.deleteTarget = city;
  }

  closeDelete() {
    if (!this.deleting) this.deleteTarget = undefined;
  }

  confirmDelete() {
    const city = this.deleteTarget;
    if (!city?.id) return;
    this.deleting = true;
    this.cityService.delete(city.id).subscribe({
      next: () => {
        this.deleting = false;
        this.deleteTarget = undefined;
        this.toastr.success(`${city.name} - ${city.state} deixou de ser atendida.`);
        this.load();
      },
      error: (err) => {
        this.deleting = false;
        this.deleteTarget = undefined;
        this.toastr.error(`Não foi possível excluir: ${backendMessage(err)}`);
      },
    });
  }
}
