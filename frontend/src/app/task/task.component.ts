import {Component, inject, Input, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Subscription} from "rxjs";
import {FormsModule} from "@angular/forms";
import {TaskService} from "./service/task.service";
import {DatePipe, KeyValuePipe, Location} from "@angular/common";
import {Message} from "../customer/message";
import {TaskDto} from "./task-dto";
import {NgbToast} from "@ng-bootstrap/ng-bootstrap";
import {TaskFormComponent} from "./task-form/task-form.component";
import {TaskConfig} from "./task-config";
import {CompleteTaskEvent} from "./CompleteTaskEvent";
import {AddAssigneeComponent} from "./add-assignee/add-assignee.component";

export interface IAddAssignee {
    username: string;
    taskId: string;
}

@Component({
    standalone: true,
    selector: 'app-task',
    templateUrl: './task.component.html',
    imports: [FormsModule, KeyValuePipe, DatePipe, TaskFormComponent, AddAssigneeComponent, NgbToast],
    styleUrls: ['./task.component.scss']
})
export class TaskComponent implements OnInit, OnDestroy {

    @Input() taskId: string = "";
    private taskService = inject(TaskService);
    private activatedRoute = inject(ActivatedRoute);
    private location = inject(Location);

    private subscription$: Subscription[] = [];
    userTaskId: string;
    task: TaskDto;
    additionalInfo: Map<string, Map<string, string>>;
    data: any;
    configData: TaskConfig;
    removeAssigneeSuccessMessage: string;
    showToast: boolean = false;
    showErrorToast: boolean = false;
    toastDelay: number = 2000;
    autohide = true;
    errorMessage: string;
    deliverFormValue: object;


    ngOnDestroy(): void {
        this.subscription$.forEach((s) => {
            s.unsubscribe();
        })
    }

    ngOnInit(): void {
        this.userTaskId = this.activatedRoute.snapshot.paramMap.get('id');
        this.getTask();
    }

    getTask() {

    }

    complete() {
        let completeTask: CompleteTaskEvent = {
            taskId: this.task.taskId,
            completeVars: this.deliverFormValue,
            taskDefinition: this.task.taskDefinition,
        }
    }

    getCardHeader(key: string): string {
        return key.replace(/([A-Z])/g, ' $1')
            .replace(/^./, string => string.toUpperCase());
    }

    showNewAssignee(assignee: IAddAssignee) {
        this.task.assignee = assignee.username;
    }

    showRemoveSuccess(message: Message) {
        if (message.text === "Assignee successfully removed") {
            this.task.assignee = null;
            this.removeAssigneeSuccessMessage = message.text;
            this.showToast = true;
        }
    }

    showDeliverFormValue(event: object) {
        console.log(event);
    }
}
