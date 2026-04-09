import {Component, DestroyRef, inject, model, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule} from "@angular/forms";
import {LoginService} from "../login.service";
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {TranslateModule} from "@ngx-translate/core";

@Component({
  selector: 'app-login',
  imports: [
    FormsModule,
    RouterLink,
    TranslateModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly loginService = inject(LoginService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly username = model('');
  readonly password = model('');
  readonly invalidLogin = signal(false);
  readonly loginSuccess = signal(false);


  login() {
    this.invalidLogin.set(false);
    this.loginService.login(this.username(), this.password())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.invalidLogin.set(false);
          this.loginSuccess.set(true);
          this.loginService.registerSuccessfulLogin(result);
          const returnTo = this.route.snapshot.queryParamMap.get('returnTo');
          this.router.navigate([returnTo ? `/${returnTo}` : '/profile']);
        },
        error: () => {
          this.invalidLogin.set(true);
          this.loginSuccess.set(false);
        }
      });
  }
}
