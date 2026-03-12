import {Component, computed, inject, input, signal} from '@angular/core';
import {GravatarService} from "../gravatar.service";

@Component({
    selector: 'app-avatar',
    standalone: true,
    templateUrl: './avatar.component.html',
    styleUrl: './avatar.component.scss'
})
export class AvatarComponent {

    avatarUrl = input<string | undefined>();
    firstName = input<string | undefined>();
    lastName = input<string | undefined>();
    username = input<string | undefined>();
    size = input<'sm' | 'md' | 'lg' | 'xl'>('md');
    shape = input<'circle' | 'square'>('circle');
    email = input<string | undefined>();
    useGravatar = input<boolean>(true);

    showInitials = signal(false);

    private gravatarService = inject(GravatarService);

    readonly initials = computed(() => {
        const first = this.firstName();
        const last = this.lastName();
        const user = this.username();
        if (first && last) return `${first.charAt(0)}${last.charAt(0)}`.toUpperCase();
        if (first) return first.charAt(0).toUpperCase();
        if (user) return user.substring(0, 2).toUpperCase();
        return '?';
    });

    readonly backgroundColor = computed(() => {
        const name = this.username() || this.firstName() || this.lastName() || 'default';
        const colors = [
            '#667eea', '#764ba2', '#f093fb', '#4facfe',
            '#43e97b', '#fa709a', '#fee140', '#30cfd0',
            '#a8edea', '#fed6e3', '#c471f5', '#fa71cd'
        ];
        let hash = 0;
        for (let i = 0; i < name.length; i++) {
            hash = name.charCodeAt(i) + ((hash << 5) - hash);
        }
        return colors[Math.abs(hash) % colors.length];
    });

    readonly displayUrl = computed(() => {
        if (this.avatarUrl()) return this.avatarUrl();
        if (this.useGravatar() && this.email()) {
            const sizeMap = {sm: 64, md: 96, lg: 128, xl: 192};
            return this.gravatarService.getGravatarUrl(this.email()!, sizeMap[this.size()], '404');
        }
        return undefined;
    });

    readonly cssClasses = computed(() =>
        `avatar avatar-${this.size()} ${this.shape() === 'circle' ? 'avatar-circle' : 'avatar-square'}`
    );

    onImageError(): void {
        this.showInitials.set(true);
    }
}
