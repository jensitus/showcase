import {Component, inject, OnInit, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {TranslateModule} from '@ngx-translate/core';
import {AdminService} from '../admin.service';
import {UserResponse} from '../../auth/user-response';

@Component({
    selector: 'app-admin-users',
    standalone: true,
    imports: [FormsModule, TranslateModule, DatePipe],
    templateUrl: './admin-users.component.html',
    styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit {

    private adminService = inject(AdminService);

    users = signal<UserResponse[]>([]);
    pendingRoles: Record<string, string> = {};
    successMessages: Record<string, boolean> = {};
    errorMessages: Record<string, boolean> = {};
    loading = signal(true);

    ngOnInit(): void {
        this.adminService.getUsers().subscribe({
            next: (users) => {
                this.users.set(users);
                users.forEach(u => this.pendingRoles[u.id] = u.role ?? 'USER');
                this.loading.set(false);
            },
            error: () => this.loading.set(false)
        });
    }

    saveRole(user: UserResponse): void {
        const newRole = this.pendingRoles[user.id];
        this.adminService.updateRole(user.id, newRole).subscribe({
            next: (updated) => {
                this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
                this.successMessages[user.id] = true;
                this.errorMessages[user.id] = false;
                setTimeout(() => this.successMessages[user.id] = false, 3000);
            },
            error: () => {
                this.errorMessages[user.id] = true;
                this.successMessages[user.id] = false;
                setTimeout(() => this.errorMessages[user.id] = false, 3000);
            }
        });
    }
}
