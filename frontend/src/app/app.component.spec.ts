import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it(`should have the 'frontend' title`, () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.title).toEqual('frontend');
  });

  it('should render title', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Hello, frontend');
  });

  it('deveEsconderOMenuNasTelasDeAutenticacao', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;

    ['/login', '/login/senha', '/register', '/register-supplier'].forEach((rota) =>
      expect(app.hiddenMenuRoutes).toContain(rota));
  });

  it('deveEsconderFooterNasTelasDeAppEAutenticacao', async () => {
    @Component({ template: '' })
    class TelaVazia {}

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([{ path: '**', component: TelaVazia }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']) },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(AppComponent);
    const router = TestBed.inject(Router);
    const footerEm = async (rota: string) => {
      await router.navigateByUrl(rota);
      fixture.detectChanges();
      return !!(fixture.nativeElement as HTMLElement).querySelector('app-footer');
    };

    for (const rota of ['/', '/onde-voce-esta', '/login', '/login/senha', '/register', '/recuperar-senha', '/entregas', '/entregas/historico', '/entregas/42']) {
      expect(await footerEm(rota)).withContext(rota).toBeFalse();
    }
    for (const rota of ['/sobre', '/contato', '/inicio', '/meus-pedidos?x=1', '/dashboard']) {
      expect(await footerEm(rota)).withContext(rota).toBeTrue();
    }
  });
});
