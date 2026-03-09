import {Component, EventEmitter, inject, Input, OnDestroy, Output} from '@angular/core';
import {IAddAssignee} from "../task.component";
import {Subscription} from "rxjs";
import {TaskService} from "../service/task.service";
import {NgbDropdown, NgbDropdownItem, NgbDropdownMenu, NgbDropdownToggle} from "@ng-bootstrap/ng-bootstrap";
import {RouterLink} from "@angular/router";
import {Message} from "../../customer/message";


@Component({
  selector: 'app-add-assignee',
  standalone: true,
  imports: [
    NgbDropdown,
    NgbDropdownItem,
    NgbDropdownMenu,
    NgbDropdownToggle,
    RouterLink
  ],
  templateUrl: './add-assignee.component.html',
  styleUrl: './add-assignee.component.scss'
})
export class AddAssigneeComponent implements OnDestroy {
  users: string[] = ["Beate Huber-Holkovics", "Verena Fitzinger", "Jens Kornacker", "Gerhard Wieshammer", "Andi Gegendorfer", "Christoph Kösner", "Dragana Sunaric", "Marlies Rieder", "Leo Li"]
  private subscription$: Subscription[] = [];
  private taskService = inject(TaskService);
  @Input() taskId: string;
  @Input() label: string = "Set Assignee";
  @Input() assignee: string | undefined;
  @Output() formValue = new EventEmitter<IAddAssignee>();
  @Output() removeSuccess = new EventEmitter<Message>();

  addAssignee(username: string) {
    let addAssignee: IAddAssignee = {
      username: username,
      taskId: this.taskId,
    }
    this.subscription$.push(

    )
  }

  ngOnDestroy(): void {
    this.subscription$.forEach((sub: Subscription) => {
      sub.unsubscribe();
    })
  }

  removeAssignee() {

  }
}
