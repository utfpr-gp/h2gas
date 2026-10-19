/**
 * F7 §5.10: texto do erro para o toast. O GlobalExceptionHandler devolve { error } para regra de
 * negócio e { message } para os demais; status 0 é falta de conexão (o corpo é um ProgressEvent).
 */
export function backendMessage(err: any, fallback = 'tente novamente em instantes'): string {
  const body = err?.error;
  // ErrorResponse (tem status): `error` é só o nome do status ("Unauthorized"); o texto está em `message`
  if (typeof body?.status === 'number' && typeof body?.message === 'string' && body.message.trim()) {
    return body.message;
  }
  if (typeof body?.error === 'string' && body.error.trim()) {
    return body.error;
  }
  if (typeof body?.message === 'string' && body.message.trim()) {
    return body.message;
  }
  if (err?.status === 0) {
    return 'sem conexão com o servidor';
  }
  return fallback;
}
