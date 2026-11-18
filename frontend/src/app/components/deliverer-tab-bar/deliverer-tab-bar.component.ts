import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

interface DelivererTab {
  id: string;
  label: string;
  icon: string;
  path: string;
}

// HU07 (Figura 17): abas Entregas | Histórico do módulo móvel do entregador
@Component({
  selector: 'app-deliverer-tab-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './deliverer-tab-bar.component.html',
})
export class DelivererTabBarComponent {
  readonly tabs: DelivererTab[] = [
    { id: 'entregas', label: 'Entregas', icon: 'two_wheeler', path: '/entregas' },
    { id: 'historico', label: 'Histórico', icon: 'history', path: '/entregas/historico' },
  ];

  constructor(private router: Router) {}

  // o detalhe da entrega tem a própria barra de ações fixa no rodapé
  get visible() {
    const path = (this.router.url || '/entregas').split('?')[0];
    return path === '/entregas' || path === '/entregas/historico';
  }

  isActive(tab: DelivererTab) {
    return (this.router.url || '/entregas').split('?')[0] === tab.path;
  }

  navigateTo(tab: DelivererTab) {
    this.router.navigate([tab.path]);
  }
}
