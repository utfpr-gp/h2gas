import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfigurationService } from '../../../services/configuration.service';
import { ToastrService } from 'ngx-toastr';
import { SupplierService } from '../../../services/supplier.service';
import { UserService } from '../../../services/user.service';
import { User } from '../../../models/user';
import { Supplier } from '../../../models/supplier';

@Component({
  selector: 'app-configuration',
  imports: [
    FormsModule,
    CommonModule,
    ReactiveFormsModule,
  ],
  templateUrl: './configuration.component.html',
  styleUrl: './configuration.component.css'
})
export class ConfigurationComponent {
  constructor(
    private configurationService: ConfigurationService,
    private fb: FormBuilder, 
    private toastr: ToastrService,
    private supplierService: SupplierService,
    private userService: UserService
  ) {
    this.formSettingsStock = this.fb.group({
      estoqueMin: [this.settings.estoqueMin, [Validators.min(1)]],
    })
    this.formHours = this.fb.group({
      openingTime:[''],
      closingTime: [''],
      averageDeliveryTime: [null, [Validators.min(1)]],
    })
  }

  formSettingsStock!: FormGroup;
  formHours!: FormGroup;

  settings = {
    estoqueMin: 0,
  };

  timeOpeningAndClose = {
    openingTime: '',
    closingTime: '',
    averageDeliveryTime: 1
  }

  userLogado?: User;
  // supplier?: Supplier;

  ngOnInit() {
    const saved = this.configurationService.getSettings();
    if (saved) {
      this.settings = saved;
      this.formSettingsStock.patchValue(this.settings);
    }

    this.userLogado = this.userService.getUserLocalStorage();
    this.getSupplierId();
  }

  getSupplierId() {
    this.supplierService.findById(this.userLogado?.supplierId!).subscribe({
      next: (response) => {
        this.formHours.patchValue({
          openingTime: response.openingTime,
          closingTime: response.closingTime,
          averageDeliveryTime: response.averageDeliveryTime
        });
        this.toastr.success('Distribuidor buscado com sucesso!');
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar: ${err.error.message}`)
      }
    })
  }

  salvar() {
    if (this.formSettingsStock.valid) {
      this.settings.estoqueMin = this.formSettingsStock.value.estoqueMin;
      this.configurationService.saveSettings(this.settings);
      console.log(this.settings);
    } else {
      this.toastr.warning("Digite um valor maior que 0")
      console.error("Formulário inválido!");
    }
    
    this.timeOpeningAndClose = {
      openingTime: this.formHours.value.openingTime,
      closingTime: this.formHours.value.closingTime,
      averageDeliveryTime: this.formHours.value.averageDeliveryTime
    };


    this.supplierService.updateTimesOperation(this.timeOpeningAndClose, this.userLogado?.supplierId!).subscribe({
      next: (response: any) => {
        console.log('Atualizado com sucesso', response);
        
        this.toastr.success(response.message);
      },
      error: (err) => {
        
        console.error('Erro ao atualizar:', err.error.message);
        this.toastr.error(`Erro ao atualizar: ${err.error.message}`)
      },
    })

  }
}
