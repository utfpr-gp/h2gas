import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { VisitorCepPanelComponent } from '../../visitor-cep-panel/visitor-cep-panel.component';

// HU13: mesma porta de entrada da landing — usada pela guarda e pelo "trocar CEP" do menu
@Component({
  selector: 'app-visitor-location',
  standalone: true,
  imports: [VisitorCepPanelComponent, RouterLink],
  templateUrl: './visitor-location.component.html',
  styleUrl: '../initial-screen/initial-screen.component.css',
})
export class VisitorLocationComponent {}
