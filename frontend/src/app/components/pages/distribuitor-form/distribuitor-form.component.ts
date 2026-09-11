import { Component, OnInit } from '@angular/core';
import { MenuComponent } from '../../menu/menu.component';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgxMaskDirective } from 'ngx-mask';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { ActivatedRoute, Router } from '@angular/router';
import { SupplierService } from '../../../services/supplier.service';
import { Supplier } from '../../../models/supplier';
import { CityService } from '../../../services/city.service';
import { City } from '../../../models/city';
import { Address } from '../../../models/address';
import { AddressService } from '../../../services/address.service';
import { UserService } from '../../../services/user.service';
import { User } from '../../../models/user';

@Component({
  selector: 'app-distribuitor-create',
  imports: [
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
    NgxMaskDirective
  ],
  templateUrl: './distribuitor-form.component.html',
  styleUrl: './distribuitor-form.component.css'
})
export class DistribuitorFormComponent implements OnInit {
  registerFormSupplier!: FormGroup;

  registerFormAddress!: FormGroup;


  supplier?: Supplier;

  cities?: City[];

  address?: Address;

  userLogado?: User;

  isEdit: boolean = false;
  idEdit: string = '';
  cidadeEncontrada: any;

  constructor(
    private fb: FormBuilder,
    private toastr: ToastrService,
    private router: Router,
    private supplierService: SupplierService,
    private cityService: CityService,
    private addressService: AddressService,
    private userService: UserService,
    private routeActive: ActivatedRoute
  ) {
    this.registerFormSupplier = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      phone: ['', [Validators.required]],
      description: ['', [Validators.required]],
      active: [false],
      cityId: [null, [Validators.required]],
      addressId: [null],
      openingTime: [''],
      closingTime: [''],
      averageDeliveryTime: [null],
      userId: [null]
    })


    this.registerFormAddress = this.fb.group({
      streetName: ['', [Validators.required]],
      number: ['', [Validators.required]],
      complement: ['', Validators.required],
      neighborhood: ['', Validators.required],
      zipCode: ['', Validators.required],
      cityId: [null, [Validators.required]],
      userId: [null],
    })
  }

  // selectedAddress: boolean = null;

  showAddressModal = false;

  ngOnInit(): void {
    this.idEdit = this.routeActive.snapshot.paramMap.get('id') || '';

    if (this.idEdit) {
      this.isEdit = true;
      this.getSupplierId(this.idEdit);
    }
    this.getCities();
    this.userLogado = this.userService.getUserLocalStorage();
    this.buscaAddressIdUser();
  }

  modalAberta = false;

  openModal() {
    if (this.address) {
      this.registerFormAddress.patchValue(this.address);
      if (this.address.cityName) {
        this.cidadeEncontrada = this.cities?.some(
          (c) => c.name!.toLowerCase() === this.address!.cityName!.toLowerCase()
        );
      }
    }

    this.modalAberta = true;
  }


  openAddressModal() {
    this.showAddressModal = true;
  }

  closeAddressModal() {
    this.showAddressModal = false;
  }

  buscarCep() {
    this.addressService.buscaViaCep(this.registerFormAddress.get('zipCode')?.value).subscribe({
      next: (response: any) => {
        this.cidadeEncontrada = this.cities?.some(c => c.name!.toLowerCase() === response.localidade.toLowerCase());

        if (!this.cidadeEncontrada) {
          this.toastr.warning(`No momento ainda não atendemos a cidade de ${response.localidade}.`);
          return;
        }
        this.toastr.success('Cep buscado com sucesso');
        this.registerFormAddress.get('streetName')?.setValue(response.logradouro)

        // this.registerFormAddress.get('cityName')?.setValue(response.localidade)
        this.registerFormAddress.get('neighborhood')?.setValue(response.bairro)

      },
      error: (err) => {
        console.log(err);
        this.toastr.error(`Erro ao buscar cep: ${err}`)
      }
    })
  }

  salvarAddress() {
    if (!this.cidadeEncontrada) {
      this.toastr.warning(`Infelizmente ainda não atendemos sua cidade.`);
      return;
    }

    if (this.registerFormAddress.valid && !this.address) {
      this.registerFormAddress.get('userId')?.setValue(this.userLogado?.id);
      const payload = { ...this.registerFormAddress.value };
      this.addressService.create(payload as Address).subscribe({
        next: (response) => {
          console.log(response);
          this.address = response;
          this.toastr.success('Seu endereço foi salvo com sucesso');
          this.closeAddressModal();
        },
        error: (err) => {
          console.log(err);
          this.toastr.error(`Erro ao salvar endereço: ${err.error.message}`);
        },
      });
    } else if (this.registerFormAddress.valid && this.address) {
      this.editAddress();
    }
  }

  editAddress() {
    const id = this.address?.id;
    if (id == null) {
      this.toastr.error('Endereço sem identificador; salve como novo ou recarregue a página.');
      return;
    }
    this.addressService.update(id, this.registerFormAddress.value).subscribe({
      next: (response) => {
        console.log(response);
        this.toastr.success('Seu endereço foi editado com sucesso');
        this.closeAddressModal();
      },
      error: (err) => {
        console.log(err);
        this.toastr.error(`Erro ao editar endereço: ${err.error.message}`);
      },
    })
  }

  buscaAddressIdUser() {
    this.addressService.findByUserId(this.userLogado?.id!!).subscribe({
      next: (response) => {
        console.log(response);
        this.address = response;
      },
      error: (err) => {
        console.log(err);
        // this.toastr.error(`Erro ao buscar endereço: ${err.error.message}`);
      },
    })
  }

  getSupplierId(id: string) {
    this.supplierService.findById(id).subscribe({
      next: (response) => {

        console.log('Distribuidor buscado com sucesso!', response);
        this.registerFormSupplier.patchValue(response);
        this.toastr.success('Distribuidor buscado com sucesso!');
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar: ${err.error.message}`)
      }
    })
  }


  register() {

    if (this.registerFormSupplier.invalid) {
      this.registerFormSupplier.markAllAsTouched();
      this.toastr.warning('Conferir informações!!')
      return;
    }


    this.supplier = this.registerFormSupplier?.value;
    this.supplier!.addressId = this.address?.id
    this.supplier!.userId = this.userLogado?.id


    this.supplierService.create(this.supplier!!).subscribe({
      next: (response) => {

        console.log('Usuário criado com sucesso!', response);
        this.toastr.success('Usuário criado com sucesso!');
        this.navigateRoute('/aguardando-aprovacao')
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar cidades: ${err.error.message}`)
      }
    })
  }

  update() {

    if (this.registerFormSupplier.invalid) {
      this.registerFormSupplier.markAllAsTouched();
      this.toastr.warning('Conferir informações!!')
      return;
    }

    this.supplier = this.registerFormSupplier?.value;
    this.supplier!!.addressId = this.address?.id


    this.supplierService.update(this.idEdit, this.supplier!!).subscribe({
      next: (response) => {

        console.log('Dsitriuidora editada com sucesso!', response);
        this.toastr.success('Distriuidora editada com sucesso!!');
        this.navigateRoute('/dashboard-fornecedor')
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar cidades: ${err.error.message}`)
      }
    })
  }

  getCities() {
    this.cityService.findAll().subscribe({
      next: (response) => {
        console.log('Buscado com sucesso', response);
        this.cities = response;
        console.log(response);

      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
      }
    })
  }

  navigateRoute(route: string) {
    this.router.navigate([route]);
  }

}
