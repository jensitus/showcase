import {Component, inject, input, output, signal} from '@angular/core';
import {AvatarService} from '../../auth/avatar.service';
import {AvatarComponent} from "../avatar/avatar.component";

@Component({
    selector: 'app-avatar-upload',
    standalone: true,
    imports: [AvatarComponent],
    templateUrl: './avatar-upload.component.html',
    styleUrl: './avatar-upload.component.scss'
})
export class AvatarUploadComponent {

    currentAvatarUrl = input<string | undefined>();
    firstName = input<string | undefined>();
    lastName = input<string | undefined>();
    username = input<string | undefined>();
    userId = input.required<number>();

    avatarUpdated = output<string>();

    uploading = signal(false);
    errorMessage = signal('');
    previewUrl = signal<string | undefined>(undefined);

    private avatarService = inject(AvatarService);

    onFileSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (!input.files?.[0]) return;

        const file = input.files[0];

        if (!file.type.startsWith('image/')) {
            this.errorMessage.set('Please select an image file');
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            this.errorMessage.set('File size must be less than 5MB');
            return;
        }

        this.errorMessage.set('');

        const reader = new FileReader();
        reader.onload = (e) => this.previewUrl.set(e.target?.result as string);
        reader.readAsDataURL(file);

        this.uploadAvatar(file);
    }

    private uploadAvatar(file: File): void {
        this.uploading.set(true);
        this.avatarService.uploadAvatar(this.userId(), file).subscribe({
            next: (response) => {
                this.uploading.set(false);
                this.avatarUpdated.emit(response.avatarUrl);
            },
            error: (error) => {
                this.uploading.set(false);
                this.errorMessage.set(error.error?.message || 'Failed to upload avatar');
                this.previewUrl.set(undefined);
            }
        });
    }

    deleteAvatar(): void {
        if (!confirm('Are you sure you want to remove your avatar?')) return;

        this.uploading.set(true);
        this.avatarService.deleteAvatar(this.userId()).subscribe({
            next: () => {
                this.uploading.set(false);
                this.previewUrl.set(undefined);
                this.avatarUpdated.emit('');
            },
            error: (error) => {
                this.uploading.set(false);
                this.errorMessage.set(error.error?.message || 'Failed to delete avatar');
            }
        });
    }

    triggerFileInput(): void {
        document.getElementById('avatar-input')?.click();
    }
}
