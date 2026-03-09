import { Injectable } from '@angular/core';
import {UserTask} from "./user-task";

export interface KnownWindowsOpened {
  [key: string]: Window;
}

export const windowsOpened: KnownWindowsOpened = {};

@Injectable({
  providedIn: 'root'
})
export class NavigateService {

  constructor() { }

  openTask(userTask: UserTask) {
    console.log("OPEN TASK", userTask);
    let previousWindow: Window | undefined = windowsOpened[userTask.id];
    if (previousWindow !== undefined) {
      if (!previousWindow.closed) {
        previousWindow.focus();
        return;
      }
      delete windowsOpened[userTask.id];
      previousWindow = undefined;
    }
    const targetWindowName = `usertask-app-${userTask.id}`;
    const targetUrl = `/tasks/${userTask.id}`;
    const targetWindow = window.open(targetUrl, targetWindowName);
    if (targetWindow) {
      windowsOpened[userTask.id] = targetWindow;
      targetWindow.focus();
    }
  };

}
