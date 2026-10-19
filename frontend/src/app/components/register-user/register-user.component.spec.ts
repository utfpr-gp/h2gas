import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { provideNgxMask } from 'ngx-mask';
import { of } from 'rxjs';

import { RegisterUserComponent } from './register-user.component';
import { UserService } from '../../services/user.service';

describe('RegisterUserComponent', () => {
  let component: RegisterUserComponent;
  let fixture: ComponentFixture<RegisterUserComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj('UserService', ['createUser']);
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']);

    await TestBed.configureTestingModule({
      imports: [RegisterUserComponent],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        provideNgxMask(),
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegisterUserComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('devePermitirSubmeterCadastroSemEndereco', () => {
    userServiceSpy.createUser.and.returnValue(of({} as any));

    component.registerForm.setValue({
      email: 'cliente@teste.com',
      name: 'Cliente Teste',
      phoneNumber: '41999999999',
      cpf: '',
      cnpj: '',
      password: '123456',
      userType: '',
    });
    component.confirmPassword = '123456';

    component.register();

    expect(userServiceSpy.createUser).toHaveBeenCalled();
    const payload = userServiceSpy.createUser.calls.mostRecent().args[0];
    expect(payload.address).toBeUndefined();
  });

  it('naoDeveTravarBotaoQuandoSenhasDiferem', () => {
    component.registerForm.setValue({
      email: 'cliente@teste.com',
      name: 'Cliente Teste',
      phoneNumber: '41999999999',
      cpf: '',
      cnpj: '',
      password: '123456',
      userType: '',
    });
    component.confirmPassword = 'diferente';

    component.register();

    expect(component.isLoadingRegister).toBeFalse();
    expect(userServiceSpy.createUser).not.toHaveBeenCalled();
    expect(toastrSpy.warning).toHaveBeenCalledWith('As senhas não conferem.');
  });
});
