import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { SupplierService } from '../../../services/supplier.service';
import { ToastrService } from 'ngx-toastr';
import { UserService } from '../../../services/user.service';
import { User } from '../../../models/user';
import { Supplier } from '../../../models/supplier';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-distributor-management',
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './distributor-management.component.html',
  styleUrl: './distributor-management.component.css'
})
export class DistributorManagementComponent implements OnInit {
  
  constructor(
    private router: Router,
    private supplierService: SupplierService,
    private toastr: ToastrService,
    private userService: UserService
  ) {

  }

  userLogado?: User;
  showSupplier = false;

  supplierDistribuidor?: Supplier;

  ngOnInit(): void {
    this.userLogado = this.userService.getUserLocalStorage();
    this.buscaDistribuidoraFornecedor(this.userLogado?.id!!);
  }

  navigateEdit(id?: number) {
    this.router.navigate(['/distributor/edit', id])
  }


  navigate(route: string) {
    this.router.navigate([route])
  }

  onToggleSupplier(checked: any) {
    debugger
    this.showSupplier = checked.target.checked;
    console.log('Distribuidora aberta?', checked);

    let data = {
      "status": this.showSupplier
    }

    this.supplierService.openDistribuidor(data, this.supplierDistribuidor?.id!).subscribe({
      next: (value) => {
        this.toastr.success("Distribuidora aberta com sucesso!!");
      },
      error: (err) => {
        this.toastr.error(`Erro ao abrir distribuidora: ${err.error.message} `);
      },
    })

  }

  buscaDistribuidoraFornecedor(userId: string) {
    debugger
    this.supplierService.findByUserId(userId).subscribe({
      next: (response) => {
        console.log('Distribuidora buscada com sucesso!', response);
        this.supplierDistribuidor = response;
        this.showSupplier = this.supplierDistribuidor.open!;
        this.toastr.success('Distribuidora buscada com sucesso!');
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        // this.toastr.error(`Erro ao buscar distribuidora: ${err.error.message}`)
      }
    })
  }
  

}
