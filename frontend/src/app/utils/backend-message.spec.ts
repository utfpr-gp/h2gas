import { backendMessage } from './backend-message';

// F7 §5.10: erro sempre com a mensagem do backend; nunca "undefined"
describe('backendMessage', () => {
  it('deveUsarErrorDoBackend', () => {
    expect(backendMessage({ status: 400, error: { error: 'Pedido já entregue.', message: 'Bad Request' } })).toBe('Pedido já entregue.');
  });

  // ErrorResponse do GlobalExceptionHandler: error é o nome do status, message é o texto real
  it('deveUsarMessageDoErrorResponse', () => {
    expect(backendMessage({ status: 401, error: { status: 401, error: 'Unauthorized', message: 'E-mail ou senha inválidos.' } }))
      .toBe('E-mail ou senha inválidos.');
  });

  it('deveUsarMessageQuandoNaoHaError', () => {
    expect(backendMessage({ status: 404, error: { message: 'Pedido não encontrado' } })).toBe('Pedido não encontrado');
  });

  it('deveAvisarSemConexaoNoStatusZero', () => {
    expect(backendMessage({ status: 0, error: new ProgressEvent('error') })).toBe('sem conexão com o servidor');
  });

  it('deveUsarTextoPadraoSemCorpo', () => {
    expect(backendMessage({ status: 500, error: null })).toBe('tente novamente em instantes');
    expect(backendMessage(undefined, 'CEP inválido')).toBe('CEP inválido');
  });
});
