import { Injectable } from '@angular/core';
import {Subject} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class SideLayoutService {

  showSidebar = new Subject<void>();
  sidebar$ = this.showSidebar.asObservable();

  constructor() { }

  showSideBarVar() {
    this.showSidebar.next();
  }

}
