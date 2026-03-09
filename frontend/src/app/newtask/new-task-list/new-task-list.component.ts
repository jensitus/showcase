import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {NewTaskService} from "../new-task.service";
import {TaskDto} from "../task.model";
import {Router} from "@angular/router";
import {LoginService} from "../../auth/login.service";
import {TranslateModule} from "@ngx-translate/core";

export type TaskFilterMode = 'my-tasks' | 'available' | 'all';

@Component({
  selector: 'app-new-task-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './new-task-list.component.html',
  styleUrl: './new-task-list.component.scss'
})
export class NewTaskListComponent implements OnInit {

    private newTaskService = inject(NewTaskService);
    private router = inject(Router);
    private loginService = inject(LoginService);

    // Signals for reactive state management
    tasks = signal<TaskDto[]>([]);
    tenantId = signal<string>('');
    currentPage = signal<number>(0);
    pageSize = signal<number>(10);
    totalElements = signal<number>(0);
    totalPages = signal<number>(0);
    sortField = signal<string>('created');
    sortDirection = signal<'asc' | 'desc'>('desc');
    loading = signal<boolean>(false);
    error = signal<string | null>(null);
    filterMode = signal<TaskFilterMode>('my-tasks');
    currentUsername = signal<string | null>(null);

    // Computed values
    hasNextPage = computed(() => this.currentPage() < this.totalPages() - 1);
    hasPreviousPage = computed(() => this.currentPage() > 0);
    startIndex = computed(() => this.currentPage() * this.pageSize() + 1);
    endIndex = computed(() =>
        Math.min((this.currentPage() + 1) * this.pageSize(), this.totalElements())
    );

    ngOnInit(): void {
        // Get current user
        const user = this.loginService.getLoggedInUserName();
        if (user) {
            this.currentUsername.set(user.username);
        }
        // Set default tenant ID or get from route/service
        this.tenantId.set('insurance');
        this.loadTasks();
    }

    loadTasks(): void {
        if (!this.tenantId()) {
            this.error.set('Please enter a tenant ID');
            return;
        }

        this.loading.set(true);
        this.error.set(null);

        // Determine assignee filter based on mode
        let assigneeFilter: string | null | undefined;
        switch (this.filterMode()) {
            case 'my-tasks':
                assigneeFilter = this.currentUsername();
                break;
            case 'available':
                assigneeFilter = null; // null means unassigned
                break;
            case 'all':
                assigneeFilter = undefined; // undefined means no filter
                break;
        }

        this.newTaskService.getTasksPaginated(
            this.tenantId(),
            this.currentPage(),
            this.pageSize(),
            this.sortField(),
            this.sortDirection(),
            assigneeFilter
        ).subscribe({
            next: (page) => {
                this.tasks.set(page.content);
                this.totalElements.set(page.totalElements);
                this.totalPages.set(page.totalPages);
                this.loading.set(false);
            },
            error: (err) => {
                this.error.set('Failed to load tasks: ' + err.message);
                this.loading.set(false);
                console.error('Error loading tasks:', err);
            }
        });
    }

    onPageChange(page: number): void {
        this.currentPage.set(page);
        this.loadTasks();
    }

    onPageSizeChange(size: number): void {
        this.pageSize.set(size);
        this.currentPage.set(0); // Reset to first page
        this.loadTasks();
    }

    onSort(field: string): void {
        if (this.sortField() === field) {
            // Toggle direction if same field
            this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
        } else {
            // New field, default to desc
            this.sortField.set(field);
            this.sortDirection.set('desc');
        }
        this.currentPage.set(0); // Reset to first page
        this.loadTasks();
    }

    onTenantIdChange(newTenantId: string): void {
        this.tenantId.set(newTenantId);
        this.currentPage.set(0);
        this.loadTasks();
    }

    onFilterModeChange(mode: TaskFilterMode): void {
        this.filterMode.set(mode);
        this.currentPage.set(0);
        this.loadTasks();
    }

    nextPage(): void {
        if (this.hasNextPage()) {
            this.onPageChange(this.currentPage() + 1);
        }
    }

    previousPage(): void {
        if (this.hasPreviousPage()) {
            this.onPageChange(this.currentPage() - 1);
        }
    }

    getSortIcon(field: string): string {
        if (this.sortField() !== field) return '↕';
        return this.sortDirection() === 'asc' ? '↑' : '↓';
    }

    viewTask(task_id) {
        this.router.navigate(['/tasks', task_id]);
    }

    editTask(task_id) {

    }

    deleteTask(task, event) {

    }

}
