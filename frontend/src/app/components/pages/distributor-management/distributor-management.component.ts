import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { SupplierService } from '../../../services/supplier.service';
import { ToastrService } from 'ngx-toastr';
import { UserService } from '../../../services/user.service';
import { User } from '../../../models/user';
import { Supplier } from '../../../models/supplier';

@Component({
  selector: 'app-distributor-management',
  imports: [],
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

  buscaDistribuidoraFornecedor(userId: string) {
    debugger
    this.supplierService.findByUserId(userId).subscribe({
      next: (response) => {
        debugger
        console.log('Distribuidora buscada com sucesso!', response);
        this.supplierDistribuidor = response;
        console.log(this.supplierDistribuidor);
        this.toastr.success('Distribuidora buscada com sucesso!');
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar distribuidora: ${err.error.message}`)
      }
    })
  }
  

}
