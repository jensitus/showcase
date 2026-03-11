import {Component, computed, EventEmitter, inject, OnInit, Output} from '@angular/core';
import {NavigationEnd, Router, RouterLink, RouterLinkActive} from "@angular/router";
import {LoginService} from "../../../../auth/login.service";
import {TranslateModule} from "@ngx-translate/core";

const SIDEBAR_COLLAPSED_KEY = 'sidebar_collapsed';

@Component({
    selector: 'app-sidebar',
    standalone: true,
    templateUrl: './sidebar.component.html',
    imports: [
        RouterLink,
        RouterLinkActive,
        TranslateModule,
    ],
    styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit {

  pushRightClass: string | undefined;
  collapsed: boolean = false;
  showMenu: string | undefined;
  isActive: boolean | undefined;
  @Output() collapsedEvent = new EventEmitter<boolean>();
  private loginService = inject(LoginService);

  readonly isLoggedIn = computed(() => this.loginService.currentUser() !== null);
  readonly isAdmin = computed(() => this.loginService.currentUser()?.role === 'ADMIN');

  constructor(public router: Router) {
    this.router.events.subscribe(val => {
      if (
        val instanceof NavigationEnd &&
        window.innerWidth <= 992 &&
        this.isToggled()
      ) {
        this.toggleSidebar();
      }
    });
  }

  ngOnInit(): void {
    this.collapsed = localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true';
    this.collapsedEvent.emit(this.collapsed);
  }

  isToggled(): boolean {
    // @ts-ignore
    const dom: Element = document.querySelector('body');
    return dom.classList.contains(<string>this.pushRightClass);
  }

  toggleSidebar() {
    const dom: any = document.querySelector('body');
    dom.classList.toggle(this.pushRightClass);
  }

  toggleCollapsed() {
    this.collapsed = !this.collapsed;
    localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(this.collapsed));
    this.collapsedEvent.emit(this.collapsed);
  }

  addExpandClass(element: string) {
    if (element === this.showMenu) {
      this.showMenu = '0';
    } else {
      this.showMenu = element;
    }
  }

  logout() {
    this.loginService.logout();
    this.router.navigate(['/login']).then();
  }
}
