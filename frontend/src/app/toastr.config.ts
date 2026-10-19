import { GlobalConfig } from 'ngx-toastr';

/**
 * F7 §5.10 (revisão 2026-10-18): um toast por vez — o novo fecha o atual (maxOpened 1 +
 * autoDismiss), nunca empilha; topo central no celular e no desktop; o mesmo texto repetido
 * só reinicia o tempo. Visual nos tokens, em styles.css.
 */
export const TOASTR_OPTIONS: Partial<GlobalConfig> = {
  positionClass: 'toast-top-center',
  maxOpened: 1,
  autoDismiss: true,
  preventDuplicates: true,
  resetTimeoutOnDuplicate: true,
  timeOut: 3500,
  extendedTimeOut: 1500,
  closeButton: true,
  progressBar: true,
  tapToDismiss: true,
  newestOnTop: true,
};
