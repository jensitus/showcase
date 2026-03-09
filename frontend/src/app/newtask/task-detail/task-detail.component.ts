import {Component, inject, OnInit, signal} from '@angular/core';
import {NewTaskService} from "../new-task.service";
import {ActivatedRoute, Router} from "@angular/router";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {TaskDto} from "../task.model";
import {DatePipe, KeyValuePipe} from "@angular/common";
import {TaskConfig} from "../../task/task-config";
import {TaskFormComponent} from "../../task/task-form/task-form.component";
import {CompleteTaskEvent} from "../../task/CompleteTaskEvent";
import {BpmnViewerComponent} from "../bpmn-viewer/bpmn-viewer.component";
import {TranslateModule} from "@ngx-translate/core";

@Component({
    selector: 'app-task-detail',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        DatePipe,
        KeyValuePipe,
        TaskFormComponent,
        BpmnViewerComponent,
        TranslateModule
    ],
    templateUrl: './task-detail.component.html',
    styleUrl: './task-detail.component.scss'
})
export class TaskDetailComponent implements OnInit {

    private taskService = inject(NewTaskService);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private fb = inject(FormBuilder);

    task = signal<TaskDto | null>(null);
    loading = signal<boolean>(false);
    error = signal<string | null>(null);
    editMode = signal<boolean>(false);
    saving = signal<boolean>(false);
    successMessage = signal<string | null>(null);
    returnTo = signal<string | null>(null);

    taskForm: FormGroup;
    additionalInfo: Map<string, Map<string, string>>;
    data: any;
    configData: TaskConfig;
    deliverFormValue: object;

    constructor() {
        this.taskForm = this.fb.group({
            name: ['', Validators.required],
            assignee: [''],
            taskState: ['', Validators.required],
            formKey: ['']
        });
    }

    ngOnInit(): void {
        this.returnTo.set(this.route.snapshot.queryParamMap.get('returnTo'));
        const taskId = this.route.snapshot.paramMap.get('id');
        if (taskId) {
            this.loadTask(taskId);
        } else {
            this.error.set('No task ID provided');
        }
    }

    loadTask(id: string): void {
        this.loading.set(true);
        this.error.set(null);

        this.taskService.getTaskById(id).subscribe({
            next: (task) => {
                this.task.set(task);
                this.populateForm(task);
                this.loading.set(false);
                this.additionalInfo = JSON.parse(task.additionalInfo);
                this.data = JSON.parse(task.config);
                this.configData = JSON.parse(task.configData);
            },
            error: (err) => {
                this.error.set('Failed to load task: ' + err.message);
                this.loading.set(false);
                console.error('Error loading task:', err);
            }
        });
    }

    populateForm(task: TaskDto): void {
        this.taskForm.patchValue({
            name: task.name,
            assignee: task.assignee,
            taskState: task.taskState,
            formKey: task.formKey
        });
    }

    toggleEditMode(): void {
        if (this.editMode()) {
            // Cancel edit - reset form
            if (this.task()) {
                this.populateForm(this.task()!);
            }
            this.editMode.set(false);
        } else {
            this.editMode.set(true);
        }
        this.successMessage.set(null);
    }

    onSubmit(): void {
        if (this.taskForm.invalid || !this.task()) {
            return;
        }

        this.saving.set(true);
        this.error.set(null);
        this.successMessage.set(null);

        const updateRequest = {
            name: this.taskForm.value.name,
            assignee: this.taskForm.value.assignee,
            taskState: this.taskForm.value.taskState,
            formKey: this.taskForm.value.formKey
        };

        this.taskService.updateTask(this.task()!.taskId, updateRequest).subscribe({
            next: (updatedTask) => {
                this.task.set(updatedTask);
                this.saving.set(false);
                this.editMode.set(false);
                this.successMessage.set('Task updated successfully!');
                setTimeout(() => this.successMessage.set(null), 3000);
            },
            error: (err) => {
                this.error.set('Failed to update task: ' + err.message);
                this.saving.set(false);
                console.error('Error updating task:', err);
            }
        })
    }

    onDelete(): void {
        if (!this.task() || !confirm('Are you sure you want to delete this task?')) {
            return;
        }

        this.saving.set(true);
        this.error.set(null);

        this.taskService.deleteTask(this.task()!.id).subscribe({
            next: () => {
                this.router.navigate(['/tasks'], {
                    queryParams: {tenantId: this.task()!.tenantId}
                });
            },
            error: (err) => {
                this.error.set('Failed to delete task: ' + err.message);
                this.saving.set(false);
                console.error('Error deleting task:', err);
            }
        });
    }

    completeTask() {
        if (!this.task()) {
            return;
        }
        let completeTask: CompleteTaskEvent = {
            taskId: this.task().taskId,
            completeVars: this.deliverFormValue,
            taskDefinition: this.task().taskDefinitionKey,
        }
        console.log(completeTask);
        console.log(this.task());
        this.taskService.completeTask(this.task()!.taskId, completeTask).subscribe({
            next: (response) => {
                console.log('Task completed successfully:', response);
                this.router.navigate(['/new-task-list'], {
                    queryParams: {tenantId: this.task()!.tenantId}
                });
            },
            error: (err) => {
                console.error('Error completing task:', err);
            }
        })
    }

    goBack(): void {
        if (this.returnTo() === 'profile') {
            this.router.navigate(['/profile']);
            return;
        }
        const tenantId = this.task()?.tenantId;
        if (tenantId) {
            this.router.navigate(['/new-task-list'], {queryParams: {tenantId}});
        } else {
            this.router.navigate(['/new-task-list']);
        }
    }

    getCardHeader(key: string): string {
        return key.replace(/([A-Z])/g, ' $1')
            .replace(/^./, string => string.toUpperCase());
    }

}
