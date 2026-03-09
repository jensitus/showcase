import {Component, inject, Input, OnInit} from '@angular/core';
import {NgClass, NgIf} from "@angular/common";
import {GravatarService} from "../gravatar.service";

@Component({
  selector: 'app-avatar',
  standalone: true,
    imports: [
        NgClass,
        NgIf,
    ],
  templateUrl: './avatar.component.html',
  styleUrl: './avatar.component.scss'
})
export class AvatarComponent implements OnInit {
    @Input() avatarUrl?: string;
    @Input() firstName?: string;
    @Input() lastName?: string;
    @Input() username?: string;
    @Input() size: 'sm' | 'md' | 'lg' | 'xl' = 'md';
    @Input() shape: 'circle' | 'square' = 'circle';
    @Input() email?: string;
    @Input() useGravatar: boolean = true;

    initials: string = '';
    backgroundColor: string = '';
    displayUrl?: string;
    showInitials: boolean = false;

    private gravatarService = inject(GravatarService);

    ngOnInit(): void {
        this.initials = this.generateInitials();
        this.backgroundColor = this.generateColorFromName();
        this.displayUrl = this.getDisplayUrl();
    }

    private getDisplayUrl(): string | undefined {
        // Priority: custom avatar URL > Gravatar > initials
        if (this.avatarUrl) {
            return this.avatarUrl;
        }

        if (this.useGravatar && this.email) {
            const sizeMap = { sm: 64, md: 96, lg: 128, xl: 192 };
            return this.gravatarService.getGravatarUrl(this.email, sizeMap[this.size], '404');
        }

        return undefined;
    }

    private generateInitials(): string {
        if (this.firstName && this.lastName) {
            return `${this.firstName.charAt(0)}${this.lastName.charAt(0)}`.toUpperCase();
        }
        if (this.firstName) {
            return this.firstName.charAt(0).toUpperCase();
        }
        if (this.username) {
            return this.username.substring(0, 2).toUpperCase();
        }
        return '?';
    }

    private generateColorFromName(): string {
        const name = this.username || this.firstName || this.lastName || 'default';
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
    }

    onImageError(): void {
        // If Gravatar fails (404), show initials
        this.showInitials = true;
        this.displayUrl = undefined;
    }

    get sizeClass(): string {
        const sizes = {
            'sm': 'avatar-sm',
            'md': 'avatar-md',
            'lg': 'avatar-lg',
            'xl': 'avatar-xl'
        };
        return sizes[this.size];
    }

    get shapeClass(): string {
        return this.shape === 'circle' ? 'avatar-circle' : 'avatar-square';
    }

}
