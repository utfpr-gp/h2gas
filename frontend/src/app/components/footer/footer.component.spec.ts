import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterLink, provideRouter } from '@angular/router';

import { FooterComponent } from './footer.component';

describe('FooterComponent', () => {
  let component: FooterComponent;
  let fixture: ComponentFixture<FooterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FooterComponent],
      providers: [provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(FooterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deveMostrarOAnoAtual', () => {
    const ano = String(new Date().getFullYear());

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(`© ${ano} H2Gás`);
  });

  it('deveUsarRouterLinkNosLinks', () => {
    const links = fixture.debugElement.queryAll(By.css('a'));

    expect(links.map((l) => l.nativeElement.getAttribute('href'))).toEqual(['/sobre', '/contato']);
    links.forEach((l) => expect(l.injector.get(RouterLink, null)).not.toBeNull());
  });

  it('naoDeveTerHexLiteral', () => {
    const classes = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('[class]'))
      .map((el) => el.getAttribute('class'))
      .join(' ');

    expect(classes).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(classes).toContain('bg-primary');
  });
});
