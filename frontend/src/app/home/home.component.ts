import {Component, computed, inject} from '@angular/core';
import {RouterLink} from "@angular/router";
import {TranslateModule} from "@ngx-translate/core";
import {LoginService} from "../auth/login.service";

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [TranslateModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {
  private loginService = inject(LoginService);

  /** Signed in: the card leads into the showcase instead of asking to register. */
  readonly isLoggedIn = computed(() => this.loginService.currentUser() !== null);
}
