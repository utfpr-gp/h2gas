import { Component, OnInit } from '@angular/core';
import { DeleteConfirmationModalComponent } from '../../delete-confirmation-modal/delete-confirmation-modal.component';
import { DynamicTableComponent } from '../../dynamic-table/dynamic-table.component';
import { Supplier } from '../../../models/supplier';
import { SupplierService } from '../../../services/supplier.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-supplier-approval',
  imports: [
    DynamicTableComponent
  ],
  templateUrl: './supplier-approval.component.html',
  styleUrl: './supplier-approval.component.css'
})
export class SupplierApprovalComponent implements OnInit {

  suppliers?: Supplier[];

  supplierChangeActive?: Supplier;

  columns = [
    {key: 'name', label: 'Nome'},
    {key: 'phone', label: 'Telefone'},
    {key: 'workingHours', label: 'Tempo de funcionamento'},
    // {key: 'averageDeliveryTime', label: 'Tempo de entrega'},
    {key: 'active', label: 'Ativo', type: 'switch'},
    {key: 'cityName', label: 'Cidade de atuação'},
    {key: 'addressNeighborhood', label: 'Bairro de atuação'},
    {key: 'addressStreetName', label: 'Rua'},
    {key: 'addressNumber', label: 'Numero endereço'},
    {key: 'user.name', label: 'Usuário Proprietário'},
    {key: 'user.email', label: 'Email'},
    {key: 'user.phoneNumber', label: 'Telefone Proprietário'},
  ]

//   actions = [
//   {
//     label: 'Toggle Ativo',
//     icon: 'fa fa-toggle-on',
//     class: 'bg-blue-500',
//     onClick: (row: any) => this.toggleFornecedor(row)
//   },
//   // outras ações, se houver
// ];
  constructor(
    private supplierService: SupplierService,
    private toastr: ToastrService,
  ) {
    this.getSuppliers();
  }

  ngOnInit(): void {
  }

  toggleFornecedor(row: any) {
    console.log(row);
    
  }

  getSuppliers() {
    debugger
    this.supplierService.findAll().subscribe({
      next: (response) => {
        console.log('Buscado com sucesso', response);
        this.suppliers = response
        console.log(this.suppliers);
        
        this.toastr.success('Fornecedores buscados com sucesso!');
      },
      error: (err) => {
        debugger
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar fornecedores: ${err.error.message}`)
      },
    })
  }

  handleSwitchChange(event: { row: any; key: string; value: boolean }) {
    debugger
    console.log('Switch alterado no pai:', event);
  
    event.row[event.key] = event.value;

    this.supplierChangeActive = event.row;

    console.log("aqui o novo supplier");
    
    console.log(this.supplierChangeActive);

    this.activeSupplier(this.supplierChangeActive!!);
  }

  activeSupplier(supplier: Supplier) {
    debugger
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
        debugger
        console.error('Erro ao atualizar:', err.error.message);
        this.toastr.error(`Erro ao buscar fornecedores: ${err.error.message}`)
      },
    })

  }

}
