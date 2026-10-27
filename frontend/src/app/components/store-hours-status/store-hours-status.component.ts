import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SupplierService } from '../../services/supplier.service';
import { UserService } from '../../services/user.service';

// HU18: a loja abre e fecha sozinha pelo horário cadastrado; aqui o fornecedor só vê o estado e o próximo horário
@Component({
  selector: 'app-store-hours-status',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './store-hours-status.component.html'
})
export class StoreHoursStatusComponent implements OnInit {
  open: boolean | null = null;
  openingTime?: string;
  closingTime?: string;

  constructor(private supplierService: SupplierService, private userService: UserService) {}

  ngOnInit(): void {
    const supplierId = this.userService.getUserLocalStorage()?.supplierId;
    if (!supplierId) return;
    this.supplierService.getOpen(supplierId).subscribe({ next: (open) => (this.open = !!open) });
    this.supplierService.getMySettings().subscribe({
      next: (settings) => {
        this.openingTime = settings.openingTime;
        this.closingTime = settings.closingTime;
      },
    });
  }
}
