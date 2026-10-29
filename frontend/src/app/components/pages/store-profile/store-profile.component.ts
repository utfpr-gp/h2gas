import { Component, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NgxMaskDirective } from 'ngx-mask';
import { ToastrService } from 'ngx-toastr';
import { SupplierService } from '../../../services/supplier.service';
import { AddressService } from '../../../services/address.service';
import { CityService } from '../../../services/city.service';
import { UserService } from '../../../services/user.service';
import { City } from '../../../models/city';
import { BANNER_MAX_BYTES, LOGO_MAX_BYTES, LOGO_TYPES, SupplierProfile, SupplierProfileRequest } from '../../../models/supplier-profile';
import { backendMessage } from '../../../utils/backend-message';
import { initials } from '../../../utils/initials';
import { matchCityFromCep } from '../../../utils/city-match';
import { fadeIn } from '../../../animations/checkout.animations';
import { ConfigurationComponent } from '../configuration/configuration.component';

type SectionId = 'aparencia' | 'dados' | 'endereco' | 'funcionamento' | 'estoque';

const LOGO_ERROR = 'Envie uma imagem JPG, PNG ou WEBP de até 1 MB.';
const BANNER_ERROR = 'Envie uma imagem JPG, PNG ou WEBP de até 2 MB.';

// F7 fatia 6 + F9: configurações da loja numa tela só (aparência, dados, endereço, funcionamento e estoque)
@Component({
  selector: 'app-store-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgxMaskDirective, ConfigurationComponent],
  templateUrl: './store-profile.component.html',
  host: { class: 'flex-1 flex flex-col' },
  animations: [fadeIn],
})
export class StoreProfileComponent implements OnInit, OnDestroy {
  readonly logoAccept = LOGO_TYPES.join(',');
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(80), Validators.pattern(/\S/)]],
    phone: ['', [Validators.required, Validators.pattern(/^\d{10,11}$/)]],
    description: ['', Validators.maxLength(500)],
  });

  profile?: SupplierProfile;
  loading = true;
  loadFailed = false;
  saving = false;
  private saved?: SupplierProfileRequest;

  logoPreview?: string;
  uploadingLogo = false;
  logoError = '';
  confirmRemoveLogo = false;
  removingLogo = false;
  // F7 §5.26: banner do topo da página da loja
  bannerPreview?: string;
  uploadingBanner = false;
  bannerError = '';
  confirmRemoveBanner = false;
  removingBanner = false;
  bannerBroken = false;

  addressOpen = false;
  savingAddress = false;
  cities: City[] = [];
  readonly addressForm = this.fb.nonNullable.group({
    zipCode: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
    streetName: ['', Validators.required],
    number: ['', Validators.required],
    complement: [''],
    neighborhood: ['', Validators.required],
  });
  addressCity?: City;
  searchingCep = false;

  // F10: uma seção por vez, escolhida na barra de etapas (como o checkout); o # da URL acompanha
  readonly sections: { id: SectionId; label: string }[] = [
    { id: 'aparencia', label: 'Aparência' },
    { id: 'dados', label: 'Dados' },
    { id: 'endereco', label: 'Endereço' },
    { id: 'funcionamento', label: 'Horário' },
    { id: 'estoque', label: 'Estoque' },
  ];
  activeSection: SectionId = 'aparencia';

  // F10: o aviso de "não salvo" da barra lê o estado das seções de horário e estoque
  @ViewChild(ConfigurationComponent) private settings?: ConfigurationComponent;

  constructor(
    private supplierService: SupplierService,
    private addressService: AddressService,
    private cityService: CityService,
    private userService: UserService,
    private toastr: ToastrService,
    private route: ActivatedRoute,
    private router: Router,
  ) {
    const fragment = this.route.snapshot.fragment;
    if (this.sections.some((sec) => sec.id === fragment)) this.activeSection = fragment as SectionId;
  }

  select(id: SectionId) {
    if (id === this.activeSection) return;
    this.activeSection = id;
    this.router.navigate([], { relativeTo: this.route, fragment: id, replaceUrl: true, queryParamsHandling: 'preserve' });
    // no celular a barra fica no topo; a seção nova começa logo abaixo dela
    if (typeof window !== 'undefined' && window.scrollY > 0) window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  hasUnsaved(id: SectionId): boolean {
    if (id === 'dados') return this.changed;
    if (id === 'funcionamento') return !!this.settings?.changed('hours');
    if (id === 'estoque') return !!this.settings?.changed('stock');
    return false;
  }

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.clearPreview();
    this.clearBannerPreview();
  }

  load() {
    this.loading = true;
    this.loadFailed = false;
    this.supplierService.getMyProfile().subscribe({
      next: (profile) => {
        this.applyProfile(profile);
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.loadFailed = true;
        this.toastr.error(`Erro ao carregar os dados da loja: ${backendMessage(err)}`);
      },
    });
  }

  private applyProfile(profile: SupplierProfile) {
    this.profile = profile;
    this.saved = { name: profile.name ?? '', phone: profile.phone ?? '', description: profile.description ?? '' };
    this.form.reset(this.saved);
  }

  get initials(): string {
    return initials(this.form.controls.name.value || this.profile?.name);
  }

  // logo que não carrega (URL velha, sem rede) volta para as iniciais
  logoBroken = false;

  get logoSrc(): string | null {
    if (this.logoPreview) return this.logoPreview;
    return this.logoBroken ? null : this.profile?.logoUrl ?? null;
  }

  get bannerSrc(): string | null {
    if (this.bannerPreview) return this.bannerPreview;
    return this.bannerBroken ? null : this.profile?.bannerUrl ?? null;
  }

  get formattedCnpj(): string {
    const d = (this.profile?.cnpj ?? '').replace(/\D/g, '');
    return d.length === 14 ? d.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5') : (this.profile?.cnpj ?? '');
  }

  get addressLine(): string {
    const p = this.profile;
    if (!p?.addressId) return '';
    const street = [p.addressStreetName, p.addressNumber].filter(Boolean).join(', ');
    const city = [p.cityName, p.cityState].filter(Boolean).join(' - ');
    return [street, p.addressNeighborhood, city].filter(Boolean).join(' · ');
  }

  // ---- dados ----
  invalid(control: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[control];
    return c.invalid && (c.touched || c.dirty);
  }

  private request(): SupplierProfileRequest {
    const v = this.form.getRawValue();
    return { name: v.name.trim(), phone: v.phone, description: v.description.trim() };
  }

  get changed(): boolean {
    const r = this.request();
    return !!this.saved && (r.name !== this.saved.name || r.phone !== this.saved.phone || r.description !== (this.saved.description ?? ''));
  }

  get canSave(): boolean {
    return this.changed && this.form.valid && !this.saving;
  }

  save() {
    if (!this.canSave) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.supplierService.updateMyProfile(this.request()).subscribe({
      next: (profile) => {
        this.saving = false;
        this.applyProfile({ ...this.profile, ...profile });
        // nome e telefone da loja são os da conta (Supplier herda de User): o menu acompanha
        const user = this.userService.getUserLocalStorage();
        if (user) this.userService.setCurrentUser({ ...user, name: profile.name, phoneNumber: profile.phone });
        this.toastr.success('Dados da loja salvos.');
      },
      error: (err) => {
        this.saving = false;
        this.toastr.error(`Não foi possível salvar: ${backendMessage(err)}`);
      },
    });
  }

  // ---- logo: envia assim que escolhe, separado do salvar ----
  onLogoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (!LOGO_TYPES.includes(file.type) || file.size > LOGO_MAX_BYTES || file.size === 0) {
      this.logoError = LOGO_ERROR;
      return;
    }
    this.logoError = '';
    this.clearPreview();
    this.logoPreview = typeof URL.createObjectURL === 'function' ? URL.createObjectURL(file) : undefined;
    this.uploadingLogo = true;
    this.supplierService.uploadMyLogo(file).subscribe({
      next: ({ logoUrl }) => {
        this.uploadingLogo = false;
        this.clearPreview();
        this.logoBroken = false;
        if (this.profile) this.profile = { ...this.profile, logoUrl };
        this.toastr.success('Logo atualizada.');
      },
      error: (err) => {
        this.uploadingLogo = false;
        this.clearPreview();
        this.toastr.error(`Não foi possível enviar a logo: ${backendMessage(err)}`);
      },
    });
  }

  removeLogo() {
    this.removingLogo = true;
    this.supplierService.removeMyLogo().subscribe({
      next: () => {
        this.removingLogo = false;
        this.confirmRemoveLogo = false;
        if (this.profile) this.profile = { ...this.profile, logoUrl: null };
        this.toastr.success('Logo removida.');
      },
      error: (err) => {
        this.removingLogo = false;
        this.confirmRemoveLogo = false;
        this.toastr.error(`Não foi possível remover a logo: ${backendMessage(err)}`);
      },
    });
  }

  // ---- banner: mesmo fluxo da logo, com limite de 2 MB ----
  onBannerSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (!LOGO_TYPES.includes(file.type) || file.size > BANNER_MAX_BYTES || file.size === 0) {
      this.bannerError = BANNER_ERROR;
      return;
    }
    this.bannerError = '';
    this.clearBannerPreview();
    this.bannerPreview = typeof URL.createObjectURL === 'function' ? URL.createObjectURL(file) : undefined;
    this.uploadingBanner = true;
    this.supplierService.uploadMyBanner(file).subscribe({
      next: ({ bannerUrl }) => {
        this.uploadingBanner = false;
        this.clearBannerPreview();
        this.bannerBroken = false;
        if (this.profile) this.profile = { ...this.profile, bannerUrl };
        this.toastr.success('Banner atualizado.');
      },
      error: (err) => {
        this.uploadingBanner = false;
        this.clearBannerPreview();
        this.toastr.error(`Não foi possível enviar o banner: ${backendMessage(err)}`);
      },
    });
  }

  removeBanner() {
    this.removingBanner = true;
    this.supplierService.removeMyBanner().subscribe({
      next: () => {
        this.removingBanner = false;
        this.confirmRemoveBanner = false;
        if (this.profile) this.profile = { ...this.profile, bannerUrl: null };
        this.toastr.success('Banner removido.');
      },
      error: (err) => {
        this.removingBanner = false;
        this.confirmRemoveBanner = false;
        this.toastr.error(`Não foi possível remover o banner: ${backendMessage(err)}`);
      },
    });
  }

  private clearBannerPreview() {
    if (this.bannerPreview && typeof URL.revokeObjectURL === 'function') URL.revokeObjectURL(this.bannerPreview);
    this.bannerPreview = undefined;
  }

  private clearPreview() {
    if (this.logoPreview && typeof URL.revokeObjectURL === 'function') URL.revokeObjectURL(this.logoPreview);
    this.logoPreview = undefined;
  }

  // ---- endereço: o CEP define a cidade atendida ----
  openAddress() {
    const p = this.profile;
    this.addressOpen = true;
    this.addressForm.reset({
      zipCode: '', streetName: p?.addressStreetName ?? '', number: p?.addressNumber ?? '',
      complement: '', neighborhood: p?.addressNeighborhood ?? '',
    });
    this.addressCity = undefined;
    if (!this.cities.length) {
      this.cityService.findAll().subscribe({
        next: (cities) => {
          this.cities = cities;
          this.addressCity = cities.find((c) => c.name === p?.cityName && c.state === p?.cityState);
        },
        error: (err) => this.toastr.error(`Erro ao carregar as cidades: ${backendMessage(err)}`),
      });
    } else {
      this.addressCity = this.cities.find((c) => c.name === p?.cityName && c.state === p?.cityState);
    }
    const userId = this.userService.getUserLocalStorage()?.id;
    if (userId) {
      this.addressService.findByUserId(userId).subscribe({
        next: (a: any) => {
          this.addressForm.patchValue({ zipCode: (a?.zipCode ?? '').replace(/\D/g, ''), complement: a?.complement ?? '' });
        },
        error: () => {},
      });
    }
  }

  closeAddress() {
    if (!this.savingAddress) this.addressOpen = false;
  }

  searchCep() {
    const cep = this.addressForm.controls.zipCode.value.replace(/\D/g, '');
    if (cep.length !== 8) {
      this.addressForm.controls.zipCode.markAsTouched();
      return;
    }
    this.searchingCep = true;
    this.addressService.buscaViaCep(cep).subscribe({
      next: (r: any) => {
        this.searchingCep = false;
        if (!r || r.erro) {
          this.toastr.error('CEP não encontrado. Confira e tente novamente.');
          return;
        }
        const city = matchCityFromCep(this.cities, r);
        if (!city) {
          this.addressCity = undefined;
          this.toastr.warning(`No momento ainda não atendemos a cidade de ${r.localidade} - ${r.uf}.`);
          return;
        }
        this.addressCity = city;
        this.addressForm.patchValue({ streetName: r.logradouro || this.addressForm.controls.streetName.value, neighborhood: r.bairro || this.addressForm.controls.neighborhood.value });
      },
      error: (err) => {
        this.searchingCep = false;
        this.toastr.error(`Erro ao buscar o CEP: ${backendMessage(err)}`);
      },
    });
  }

  get canSaveAddress(): boolean {
    return this.addressForm.valid && !!this.addressCity && !this.savingAddress;
  }

  saveAddress() {
    const id = this.profile?.addressId;
    if (!id || !this.canSaveAddress) {
      this.addressForm.markAllAsTouched();
      return;
    }
    const v = this.addressForm.getRawValue();
    this.savingAddress = true;
    this.addressService.update(id, {
      ...v, zipCode: v.zipCode, cityId: this.addressCity!.id, userId: this.userService.getUserLocalStorage()?.id,
    }).subscribe({
      next: () => {
        this.savingAddress = false;
        this.addressOpen = false;
        this.toastr.success('Endereço da loja salvo.');
        this.load();
      },
      error: (err) => {
        this.savingAddress = false;
        this.toastr.error(`Não foi possível salvar o endereço: ${backendMessage(err)}`);
      },
    });
  }
}
