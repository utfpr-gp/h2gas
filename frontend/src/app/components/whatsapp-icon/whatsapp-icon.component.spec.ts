import { TestBed } from '@angular/core/testing';
import { WhatsappIconComponent } from './whatsapp-icon.component';

// HU28: os botões de WhatsApp mostram o logo, não um balão genérico
describe('WhatsappIconComponent', () => {
  it('deveDesenharOLogoDecorativoNaCorDoBotao', async () => {
    await TestBed.configureTestingModule({ imports: [WhatsappIconComponent] }).compileComponents();
    const fixture = TestBed.createComponent(WhatsappIconComponent);
    fixture.detectChanges();

    const svg = fixture.nativeElement.querySelector('svg') as SVGElement;
    expect(svg).not.toBeNull();
    expect(svg.getAttribute('aria-hidden')).toBe('true');
    expect(svg.getAttribute('fill')).toBe('currentColor');
  });
});
