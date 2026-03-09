import {Component, EventEmitter, Input, Output} from '@angular/core';
import {FormsModule, NgForm} from "@angular/forms";
import {TaskConfig} from "../task-config";
import {NgForOf, NgIf} from "@angular/common";

@Component({
  selector: 'app-task-form',
  standalone: true,
    imports: [
        FormsModule,
        NgForOf,
        NgIf,
    ],
  templateUrl: './task-form.component.html',
  styleUrl: './task-form.component.scss'
})
export class TaskFormComponent {
  @Input() data: unknown;
  @Input() config: TaskConfig;
  @Output() formStatus = new EventEmitter<boolean>();
  @Output() formValue = new EventEmitter<object>();

  getSortedObjectKeys(object: unknown): string[] {
    if (object === null || object === undefined) return [];

    return Object.keys(object).sort((a, b) => {
      return this.config[a].position - this.config[b].position;
    });
  }

  getLabel(key: string): string {
    return this.config[key]?.label ?? key.replace(/([A-Z])/g, ' $1')
                                         .replace(/^./, string => string.toUpperCase());
  }

  getType(key: string): string {
    return this.config[key]?.type ?? 'text';
  }

  isRequired(key: string): boolean {
    return this.config[key]?.required ?? true;
  }

  isVisible(key: string): boolean {
    return this.config[key]?.visible ?? true;
  }

  onFormChange(form: NgForm): void {
    this.formStatus.emit(form.valid);
    this.formValue.emit(form.value)
  }

  showChecked(event: any) {
    console.log(event);
    return event;
  }
}
