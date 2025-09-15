import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { importProvidersFrom } from '@angular/core';
import { ToastrModule } from 'ngx-toastr';
import { provideAnimations } from '@angular/platform-browser/animations';
import { authInterceptor } from './app/interceptors/auth.interceptor';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {provideNgxMask } from 'ngx-mask'


// bootstrapApplication(AppComponent, appConfig)
//   .catch((err) => console.error(err));

bootstrapApplication(AppComponent, {
  ...appConfig,
  providers: [
    ...(appConfig.providers || []),
    importProvidersFrom(
      ToastrModule.forRoot(),
    ),
    provideAnimations(),
    provideNgxMask(),
    provideHttpClient(
      withInterceptors([
        authInterceptor
      ])
    )
  ]

}).catch((err) => console.error(err));