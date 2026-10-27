import {Component, inject} from '@angular/core';
import {AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {SubmissionService} from '../submission.service';
import {ABSTRACT_SECTIONS, MAX_TOTAL} from '../abstract-sections';

/** The whole abstract may not exceed MAX_TOTAL characters, however it is spread over the sections. */
function totalLength(group: AbstractControl): ValidationErrors | null {
  return sectionsTotal(group as FormGroup) > MAX_TOTAL ? {totalTooLong: true} : null;
}

function sectionsTotal(group: FormGroup): number {
  return Object.values(group.controls).reduce((n, c) => n + ((c.value as string) ?? '').trim().length, 0);
}

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

  readonly sectionDefs = ABSTRACT_SECTIONS;
  readonly maxTotal = MAX_TOTAL;

  topics = [
    'ARTIFICIAL_INTELLIGENCE',
    'SOFTWARE_ARCHITECTURE',
    'DEVOPS',
    'CLOUD_COMPUTING',
    'SECURITY',
    'DATA_ENGINEERING',
    'OTHER'
  ];

  readonly sections: FormGroup = this.fb.group(
    Object.fromEntries(ABSTRACT_SECTIONS.map(s => [s.shortname, ['', [
      ...(s.required ? [Validators.required] : []),
      Validators.maxLength(s.maxLength),
    ]]])),
    {validators: totalLength}
  );

  form = this.fb.group({
    title: ['', Validators.required],
    authors: ['', Validators.required],
    sections: this.sections,
    topic: ['', Validators.required],
    submitterEmail: ['', [Validators.required, Validators.email]]
  });

  length(shortname: string): number {
    return ((this.sections.get(shortname)?.value as string) ?? '').length;
  }

  total(): number {
    return sectionsTotal(this.sections);
  }

  invalid(shortname: string): boolean {
    const c = this.sections.get(shortname);
    return !!c && c.invalid && c.touched;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.error = null;

    const v = this.form.getRawValue();
    this.submissionService.createSubmission({
      title: v.title!,
      authors: v.authors!,
      topic: v.topic!,
      submitterEmail: v.submitterEmail!,
      sections: ABSTRACT_SECTIONS
        .map(s => ({shortname: s.shortname, content: ((v.sections as any)[s.shortname] ?? '').trim()}))
        .filter(s => s.content.length > 0),
    }).subscribe({
      next: () => this.router.navigate(['/submissions']),
      error: (err) => {
        this.error = err.error?.message ?? err.message;
        this.submitting = false;
      }
    });
  }
}
