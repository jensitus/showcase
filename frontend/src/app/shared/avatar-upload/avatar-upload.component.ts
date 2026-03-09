import {Component, EventEmitter, inject, Input, Output} from '@angular/core';
import { AvatarService } from '../avatar.service';
import {AvatarComponent} from "../avatar/avatar.component";
import {NgIf} from "@angular/common";

@Component({
  selector: 'app-avatar-upload',
  standalone: true,
    imports: [
        AvatarComponent,
        NgIf
    ],
  templateUrl: './avatar-upload.component.html',
  styleUrl: './avatar-upload.component.scss'
})
export class AvatarUploadComponent {
    @Input() currentAvatarUrl?: string;
    @Input() firstName?: string;
    @Input() lastName?: string;
    @Input() username?: string;
    @Input() userId!: number;
    @Output() avatarUpdated = new EventEmitter<string>();

    uploading = false;
    errorMessage = '';
    previewUrl?: string;

    private avatarService = inject(AvatarService);

    onFileSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files[0]) {
            const file = input.files[0];

            // Validate file type
            if (!file.type.startsWith('image/')) {
                this.errorMessage = 'Please select an image file';
                return;
            }

            // Validate file size (max 5MB)
            if (file.size > 5 * 1024 * 1024) {
                this.errorMessage = 'File size must be less than 5MB';
                return;
            }

            this.errorMessage = '';

            // Preview
            const reader = new FileReader();
            reader.onload = (e) => {
                this.previewUrl = e.target?.result as string;
            };
            reader.readAsDataURL(file);

            // Upload
            this.uploadAvatar(file);
        }
    }

    private uploadAvatar(file: File): void {
        this.uploading = true;
        this.avatarService.uploadAvatar(this.userId, file).subscribe({
            next: (response) => {
                this.uploading = false;
                this.currentAvatarUrl = response.avatarUrl;
                this.avatarUpdated.emit(response.avatarUrl);
            },
            error: (error) => {
                this.uploading = false;
                this.errorMessage = error.error?.message || 'Failed to upload avatar';
                this.previewUrl = undefined;
            }
        });
    }

    deleteAvatar(): void {
        if (!confirm('Are you sure you want to remove your avatar?')) {
            return;
        }

        this.uploading = true;
        this.avatarService.deleteAvatar(this.userId).subscribe({
            next: () => {
                this.uploading = false;
                this.currentAvatarUrl = undefined;
                this.previewUrl = undefined;
                this.avatarUpdated.emit('');
            },
            error: (error) => {
                this.uploading = false;
                this.errorMessage = error.error?.message || 'Failed to delete avatar';
            }
        });
    }

    triggerFileInput(): void {
        document.getElementById('avatar-input')?.click();
    }

}
