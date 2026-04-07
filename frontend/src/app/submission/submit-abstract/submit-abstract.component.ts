import {Component, inject} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {SubmissionService} from '../submission.service';

@Component({
  selector: 'app-submit-abstract',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule],
  templateUrl: './submit-abstract.component.html',
  styleUrl: './submit-abstract.component.scss'
})
export class SubmitAbstractComponent {

  private fb = inject(FormBuilder);
  private submissionService = inject(SubmissionService);
  private router = inject(Router);

  submitting = false;
  error: string | null = null;

  topics = [
    'ARTIFICIAL_INTELLIGENCE',
    'SOFTWARE_ARCHITECTURE',
    'DEVOPS',
    'CLOUD_COMPUTING',
    'SECURITY',
    'DATA_ENGINEERING',
    'OTHER'
  ];

  form = this.fb.group({
    title: ['', Validators.required],
    authors: ['', Validators.required],
    abstractText: ['', [Validators.required, Validators.minLength(100)]],
    topic: ['', Validators.required],
    submitterEmail: ['', [Validators.required, Validators.email]]
  });

  submit(): void {
    if (this.form.invalid) return;
    this.submitting = true;
    this.error = null;

    this.submissionService.createSubmission(this.form.value as any).subscribe({
      next: () => this.router.navigate(['/submissions']),
      error: (err) => {
        this.error = err.message;
        this.submitting = false;
      }
    });
  }
}
