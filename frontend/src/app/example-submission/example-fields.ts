/**
 * Minimal stand-in field components for the EXAMPLE wizard. Each mirrors the
 * two-way contract the real components expose: an input `value` + an output
 * `valueChange` (AffirmationsBlock additionally `perAuthorAcceptance`). Replace
 * these with your production `@/components/fields/*` — the wizard binds them the
 * same way.
 */
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  Affiliation,
  AuthorBlockModel,
  AuthorEntry,
  EXAMPLE_AFFILIATIONS,
  TaxonomyTree,
} from './example-backend';

@Component({
  selector: 'app-ex-title-field',
  standalone: true,
  imports: [FormsModule],
  template: `
    <label class="fld">
      <span>{{ label ?? 'Title' }}</span>
      <input [ngModel]="value" (ngModelChange)="valueChange.emit($event)" type="text" />
    </label>
  `,
  styles: [
    `.fld { display: flex; flex-direction: column; gap: 0.25rem; }
     .fld > span { font-size: 0.85rem; color: #666; }
     input { padding: 0.5rem 0.6rem; font: inherit; border: 1px solid #d4d4d8; border-radius: 6px; }`,
  ],
})
export class ExTitleFieldComponent {
  @Input() value = '';
  @Input() label?: string;
  @Output() valueChange = new EventEmitter<string>();
}

@Component({
  selector: 'app-ex-text-field',
  standalone: true,
  imports: [FormsModule],
  template: `
    <label class="fld">
      <span>{{ label }}</span>
      <textarea
        [rows]="rows ?? 6"
        [placeholder]="placeholder ?? ''"
        [ngModel]="value"
        (ngModelChange)="valueChange.emit($event)"
      ></textarea>
      <small [class.ok]="value.length >= 50">{{ value.length }} / 50 min</small>
    </label>
  `,
  styles: [
    `.fld { display: flex; flex-direction: column; gap: 0.25rem; }
     .fld > span { font-size: 0.85rem; color: #666; }
     textarea { padding: 0.5rem 0.6rem; font: inherit; border: 1px solid #d4d4d8; border-radius: 6px; resize: vertical; }
     small { color: #b91c1c; } small.ok { color: #15803d; }`,
  ],
})
export class ExTextFieldComponent {
  @Input() value = '';
  @Input() label?: string;
  @Input() rows?: number;
  @Input() placeholder?: string;
  @Output() valueChange = new EventEmitter<string>();
}

@Component({
  selector: 'app-ex-taxonomy-field',
  standalone: true,
  imports: [FormsModule],
  template: `
    <label class="fld">
      <span>{{ tree.taxonomy.displayName }}</span>
      <select [ngModel]="value" (ngModelChange)="valueChange.emit($event)">
        <option value="">— choose —</option>
        @for (n of tree.nodes; track n.id) {
          <option [value]="n.id">{{ n.label }}</option>
        }
      </select>
    </label>
  `,
  styles: [
    `.fld { display: flex; flex-direction: column; gap: 0.25rem; }
     .fld > span { font-size: 0.85rem; color: #666; }
     select { padding: 0.5rem 0.6rem; font: inherit; border: 1px solid #d4d4d8; border-radius: 6px; }`,
  ],
})
export class ExTaxonomyFieldComponent {
  @Input() value = '';
  @Input() tree!: TaxonomyTree;
  @Output() valueChange = new EventEmitter<string>();
}

@Component({
  selector: 'app-ex-author-block',
  standalone: true,
  imports: [FormsModule],
  template: `
    <fieldset>
      <legend>Submitter affiliations</legend>
      <div class="affs">
        @for (a of allAffiliations; track a.id) {
          <label class="chk">
            <input
              type="checkbox"
              [checked]="hasAff(model.submitterAffiliations, a)"
              (change)="toggleAff(model.submitterAffiliations, a); emit()"
            />
            {{ a.label }}
          </label>
        }
      </div>
    </fieldset>

    <fieldset>
      <legend>Presenter (optional)</legend>
      <div class="row">
        <input placeholder="Given name" [(ngModel)]="model.presenter.givenName" (ngModelChange)="emit()" />
        <input placeholder="Family name" [(ngModel)]="model.presenter.familyName" (ngModelChange)="emit()" />
        <input placeholder="Email" [(ngModel)]="model.presenter.email" (ngModelChange)="emit()" />
      </div>
      <div class="affs">
        @for (a of allAffiliations; track a.id) {
          <label class="chk">
            <input
              type="checkbox"
              [checked]="hasAff(model.presenter.affiliations, a)"
              (change)="toggleAff(model.presenter.affiliations, a); emit()"
            />
            {{ a.label }}
          </label>
        }
      </div>
    </fieldset>

    <fieldset>
      <legend>Co-authors</legend>
      @for (c of model.coAuthors; track $index) {
        <div class="row">
          <input placeholder="Given name" [(ngModel)]="c.givenName" (ngModelChange)="emit()" />
          <input placeholder="Family name" [(ngModel)]="c.familyName" (ngModelChange)="emit()" />
          <input placeholder="Email" [(ngModel)]="c.email" (ngModelChange)="emit()" />
          <button type="button" (click)="removeCoAuthor($index)">✕</button>
        </div>
      }
      <button type="button" class="add" (click)="addCoAuthor()">+ Add co-author</button>
    </fieldset>
  `,
  styles: [
    `fieldset { border: 1px solid #e4e4e7; border-radius: 8px; margin: 0 0 0.75rem; padding: 0.75rem 1rem; }
     legend { font-size: 0.85rem; color: #666; padding: 0 0.4rem; }
     .row { display: flex; gap: 0.5rem; margin-bottom: 0.5rem; flex-wrap: wrap; }
     .row input { flex: 1 1 10rem; padding: 0.4rem 0.5rem; border: 1px solid #d4d4d8; border-radius: 6px; font: inherit; }
     .affs { display: flex; gap: 1rem; flex-wrap: wrap; }
     .chk { font-size: 0.9rem; display: inline-flex; gap: 0.35rem; align-items: center; }
     button { border: 1px solid #d4d4d8; background: #fafafa; border-radius: 6px; padding: 0.3rem 0.6rem; cursor: pointer; }
     .add { margin-top: 0.25rem; }`,
  ],
})
export class ExAuthorBlockComponent {
  allAffiliations = EXAMPLE_AFFILIATIONS;
  model: AuthorBlockModel = emptyAuthorModel();

  @Input() set value(v: AuthorBlockModel) {
    this.model = structuredClone(v);
  }
  @Output() valueChange = new EventEmitter<AuthorBlockModel>();

  emit(): void {
    this.valueChange.emit(structuredClone(this.model));
  }

  hasAff(list: Affiliation[], a: Affiliation): boolean {
    return list.some((x) => x.id === a.id);
  }
  toggleAff(list: Affiliation[], a: Affiliation): void {
    const i = list.findIndex((x) => x.id === a.id);
    if (i >= 0) list.splice(i, 1);
    else list.push({ ...a });
  }
  addCoAuthor(): void {
    this.model.coAuthors.push({ givenName: '', familyName: '', email: '', affiliations: [] });
    this.emit();
  }
  removeCoAuthor(i: number): void {
    this.model.coAuthors.splice(i, 1);
    this.emit();
  }
}

function emptyAuthorModel(): AuthorBlockModel {
  const presenter: AuthorEntry = { givenName: '', familyName: '', email: '', affiliations: [] };
  return { submitterAffiliations: [], presenter, coAuthors: [] };
}

interface ExampleAffirmation {
  id: string;
  label: string;
}

@Component({
  selector: 'app-ex-affirmations-block',
  standalone: true,
  imports: [FormsModule],
  template: `
    <p class="ctx">For <strong>{{ conferenceSlug }}</strong> · {{ submissionType }}</p>

    <fieldset>
      <legend>Submission affirmations</legend>
      @for (a of submissionAffirmations; track a.id) {
        <label class="chk">
          <input type="checkbox" [checked]="value.includes(a.id)" (change)="toggle(a.id)" />
          {{ a.label }}
        </label>
      }
    </fieldset>

    <fieldset>
      <legend>On behalf of all listed authors</legend>
      @for (a of perAuthorAffirmations; track a.id) {
        <label class="chk">
          <input type="checkbox" [checked]="!!perAuthorAcceptance[a.id]" (change)="togglePerAuthor(a.id)" />
          {{ a.label }}
        </label>
      }
    </fieldset>
  `,
  styles: [
    `.ctx { font-size: 0.85rem; color: #666; margin: 0 0 0.5rem; }
     fieldset { border: 1px solid #e4e4e7; border-radius: 8px; margin: 0 0 0.75rem; padding: 0.75rem 1rem; }
     legend { font-size: 0.85rem; color: #666; padding: 0 0.4rem; }
     .chk { display: flex; gap: 0.5rem; align-items: flex-start; margin-bottom: 0.4rem; font-size: 0.9rem; }`,
  ],
})
export class ExAffirmationsBlockComponent {
  @Input() value: string[] = [];
  @Input() perAuthorAcceptance: Record<string, boolean> = {};
  @Input() conferenceSlug = '';
  @Input() submissionType = '';
  @Output() valueChange = new EventEmitter<string[]>();
  @Output() perAuthorAcceptanceChange = new EventEmitter<Record<string, boolean>>();

  submissionAffirmations: ExampleAffirmation[] = [
    { id: 'original', label: 'This work is original and not under review elsewhere.' },
    { id: 'ethics', label: 'Ethical approval was obtained where applicable.' },
  ];
  perAuthorAffirmations: ExampleAffirmation[] = [
    { id: 'consent', label: 'All listed authors consent to this submission.' },
  ];

  toggle(id: string): void {
    const next = this.value.includes(id) ? this.value.filter((x) => x !== id) : [...this.value, id];
    this.valueChange.emit(next);
  }
  togglePerAuthor(id: string): void {
    const next = { ...this.perAuthorAcceptance, [id]: !this.perAuthorAcceptance[id] };
    this.perAuthorAcceptanceChange.emit(next);
  }
}
