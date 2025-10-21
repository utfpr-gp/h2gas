import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ConfigurationService } from '../../../services/configuration.service';

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
  constructor(private configurationService: ConfigurationService) {}

  settings = {
    estoqueAgua: 0,
    estoqueGas: 0,
  };

  ngOnInit() {
    const saved = this.configurationService.getSettings();
    if (saved) {
      this.settings = saved;
    }
  }

  salvar() {
    this.configurationService.saveSettings(this.settings);
  }
}
