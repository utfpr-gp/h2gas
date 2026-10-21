import { Component, OnInit } from '@angular/core';
import { DeleteConfirmationModalComponent } from '../../delete-confirmation-modal/delete-confirmation-modal.component';
import { DynamicTableComponent } from '../../dynamic-table/dynamic-table.component';
import { Supplier } from '../../../models/supplier';
import { SupplierService } from '../../../services/supplier.service';
import { ToastrService } from 'ngx-toastr';
import { MenuComponent } from '../../menu/menu.component';
import { backendMessage } from '../../../utils/backend-message';

@Component({
  selector: 'app-supplier-approval',
  imports: [
    DynamicTableComponent,
  ],
  templateUrl: './supplier-approval.component.html',
  host: { class: 'flex-1 flex flex-col' },
})
export class SupplierApprovalComponent implements OnInit {

  suppliers?: Supplier[];

  supplierChangeActive?: Supplier;

  columns = [
    {key: 'name', label: 'Nome'},
    {key: 'phone', label: 'Telefone'},
    // {key: 'workingHours', label: 'Tempo de funcionamento'},
    // {key: 'averageDeliveryTime', label: 'Tempo de entrega'},
    {key: 'active', label: 'Ativo', type: 'switch'},
    {key: 'cityName', label: 'Cidade de atuação'},
    {key: 'addressNeighborhood', label: 'Bairro de atuação'},
    {key: 'addressStreetName', label: 'Rua'},
    {key: 'addressNumber', label: 'Numero endereço'},
    {key: 'cnpj', label: 'CNPJ'},
    {key: 'email', label: 'Email'},
    // {key: 'user.phoneNumber', label: 'Telefone Proprietário'},
  ]
  
  constructor(
    private supplierService: SupplierService,
    private toastr: ToastrService,
  ) {
    this.getSuppliers();
  }

  ngOnInit(): void {
  }

  getSuppliers() {
    this.supplierService.findAllPaginated().subscribe({
      next: (response) => {
        console.log('Buscado com sucesso', response);
        this.suppliers = response.data
        console.log(this.suppliers);
        
      },
      error: (err) => {
        
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar fornecedores: ${backendMessage(err)}`)
      },
    })
  }

  handleSwitchChange(event: { row: any; key: string; value: boolean }) {
    event.row[event.key] = event.value;

    this.supplierChangeActive = event.row;

    this.activeSupplier(this.supplierChangeActive!!);
  }

  activeSupplier(supplier: Supplier) {
    let data = {
      "active": this.supplierChangeActive?.active
    }

    this.supplierService.activeSuppliers(data, supplier.id!!).subscribe({
      next: (response: any) => {
        console.log('Atualizado com sucesso', response);
        console.log(this.suppliers);
        
        this.toastr.success(response.message);
      },
      error: (err) => {
        
        console.error('Erro ao atualizar:', err.error.message);
        this.toastr.error(`Erro ao buscar fornecedores: ${backendMessage(err)}`)
      },
    })

  }

}
