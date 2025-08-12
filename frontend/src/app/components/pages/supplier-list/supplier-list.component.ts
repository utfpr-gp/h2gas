import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { SupplierService } from '../../../services/supplier.service';
import { Supplier } from '../../../models/supplier';
import { ToastrService } from 'ngx-toastr';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-supplier-list',
  imports: [
    FormsModule,
    CommonModule
  ],
  templateUrl: './supplier-list.component.html',
  styleUrl: './supplier-list.component.css'
})
export class SupplierListComponent implements OnInit {
  
  constructor(
    private supplierService: SupplierService,
    private toastr: ToastrService,
    private router: Router
  ) {}


  ngOnInit(): void {
    this.getSuppliers();
  }

  suppliers?: Supplier[];

  getSuppliers() {
    this.supplierService.getAllSupplierOpenAndActive().subscribe({
      next: (response) => {
        console.log('Buscado com sucesso', response);
        this.suppliers = response.data
        console.log(this.suppliers!.length);
        
        this.toastr.success('Fornecedores buscados com sucesso!');
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar fornecedores: ${err.error.message}`)
      },
    })
  }

  navigate(id?: number) {
    this.router.navigate(['/fornecedores', id, 'produtos']);
  }

}
