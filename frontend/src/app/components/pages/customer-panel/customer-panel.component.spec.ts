import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { provideNgxMask } from 'ngx-mask';

import { CustomerPanelComponent } from './customer-panel.component';
import { UserService } from '../../../services/user.service';

describe('CustomerPanelComponent', () => {
  let component: CustomerPanelComponent;
  let fixture: ComponentFixture<CustomerPanelComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    localStorage.clear();
    userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage', 'updateUser']);
    userServiceSpy.getUserLocalStorage.and.returnValue({
      id: 'user-1', name: 'Igor', email: 'igor@h2gas.test', phoneNumber: '41999999999',
    });
    userServiceSpy.updateUser.and.returnValue(of({ id: 'user-1' } as any));
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [CustomerPanelComponent],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']) },
        provideNgxMask(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deveExibirAtalhoParaMeusEnderecos', () => {
    const atalho = (fixture.nativeElement as HTMLElement).querySelector('[data-testid="atalho-meus-enderecos"]') as HTMLButtonElement;
    expect(atalho).toBeTruthy();

    atalho.click();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/meus-enderecos']);
  });

  it('deveSalvarDadosDoUsuario', () => {
    component.registerFormUser.get('name')?.setValue('Igor Emanuel');

    component.update();

    expect(userServiceSpy.updateUser).toHaveBeenCalledWith(
      'user-1',
      jasmine.objectContaining({ name: 'Igor Emanuel', email: 'igor@h2gas.test' })
    );
  });
});
