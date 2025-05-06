import { Component, OnInit } from '@angular/core';
import { DeleteConfirmationModalComponent } from '../../delete-confirmation-modal/delete-confirmation-modal.component';
import { DynamicTableComponent } from '../../dynamic-table/dynamic-table.component';
import { Supplier } from '../../../models/supplier';
import { SupplierService } from '../../../services/supplier.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-supplier-approval',
  imports: [
    DeleteConfirmationModalComponent,
    DynamicTableComponent
  ],
  templateUrl: './supplier-approval.component.html',
  styleUrl: './supplier-approval.component.css'
})
export class SupplierApprovalComponent implements OnInit {

  suppliers?: Supplier[];

  columns = [
    {key: 'name', label: 'Nome'},
    {key: 'phone', label: 'Telefone'},
    {key: 'workingHours', label: 'Tempo de funcionamento'},
    {key: 'averageDeliveryTime', label: 'Tempo de entrega'},
    {key: 'active', label: 'Ativo'},
    {key: 'cityName', label: 'Cidade de atuação'},
    {key: 'addressNeighborhood', label: 'Bairro de atuação'},
    {key: 'addressStreetName', label: 'Rua'},
    {key: 'addressNumber', label: 'Numero endereço'},
    {key: 'user.name', label: 'Usuário Proprietário'},
    {key: 'user.email', label: 'Email'},
    {key: 'user.phoneNumber', label: 'Telefone Proprietário'},
  ]

  actions = [
  {
    label: 'Toggle Ativo',
    icon: 'fa fa-toggle-on',
    class: 'bg-blue-500',
    onClick: (row: any) => this.toggleFornecedor(row)
  },
  // outras ações, se houver
];
  constructor(
    private supplierService: SupplierService,
    private toastr: ToastrService,
  ) {
    this.getSuppliers();
  }

  ngOnInit(): void {
    throw new Error('Method not implemented.');
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

}
