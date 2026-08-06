import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';

import {
  AuthStore,
  AuthorBlockModel,
  Conference,
  ConferencesApi,
  FormStep,
  FormTemplate,
  FormUiSchema,
  FormTemplatesApi,
  PeopleApi,
  SubmissionType,
  SubmissionsApi,
  TaxonomiesApi,
  TaxonomyTree,
  problemOf,
} from './example-backend';
import {
  ExAffirmationsBlockComponent,
  ExAuthorBlockComponent,
  ExTaxonomyFieldComponent,
  ExTextFieldComponent,
  ExTitleFieldComponent,
} from './example-fields';

/**
 * EXAMPLE form-template-driven submission wizard (Angular port of the Vue
 * `<script setup>` original). The active free-paper template carries a
 * `uiSchema` with a list of steps; each step contains an ordered list of field
 * kinds (title / taxonomy / text / author-block / affirmations). We render those
 * in order and assemble the create + replaceAuthors + replaceAffirmations calls
 * on Finish.
 *
 * When the template has no `uiSchema` we synthesise a default: one step with
 * every schema property + required taxonomies, then Authors, then Affirmations.
 *
 * This example uses in-memory mocks (see example-backend.ts) and, instead of
 * navigating to submission-edit, shows the assembled payload so you can inspect
 * what the wizard builds. Swap `tr()` for ngx-translate and the mock injects for
 * your real services to productionise.
 */
@Component({
  selector: 'app-example-submission',
  standalone: true,
  imports: [
    FormsModule,
    JsonPipe,
    ExTitleFieldComponent,
    ExTextFieldComponent,
    ExTaxonomyFieldComponent,
    ExAuthorBlockComponent,
    ExAffirmationsBlockComponent,
  ],
  templateUrl: './example-submission.component.html',
  styleUrl: './example-submission.component.scss',
})
export class ExampleSubmissionComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly auth = inject(AuthStore);

  private readonly conferencesApi = inject(ConferencesApi);
  private readonly peopleApi = inject(PeopleApi);
  private readonly submissionsApi = inject(SubmissionsApi);
  private readonly formTemplatesApi = inject(FormTemplatesApi);
  private readonly taxonomiesApi = inject(TaxonomiesApi);

  // ── Top-level state ─────────────────────────────────────────────────────

  readonly conferences = signal<Conference[]>([]);
  readonly slug = signal<string>('');
  readonly type = signal<SubmissionType>('FREE_PAPER');
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  /** Example-only: assembled result shown instead of navigating away. */
  readonly result = signal<unknown | null>(null);
  /**
   * Example-only: the same submission projected to the novelty screener's input
   * shape — a JSON **array** of `{id, title, abstract}`. The screener's paste box
   * accepts a JSON array or JSONL; a single object (like `result`) does not parse.
   */
  readonly screenerInput = signal<Array<{ id: string; title: string; abstract: string }> | null>(null);

  // ── State shared across steps ───────────────────────────────────────────

  readonly title = signal('');
  readonly payload = signal<Record<string, string>>({});
  readonly selectedNodeByTaxonomy = signal<Record<string, string>>({});
  readonly authorModel = signal<AuthorBlockModel>({
    submitterAffiliations: [],
    presenter: { givenName: '', familyName: '', email: '', affiliations: [] },
    coAuthors: [],
  });
  readonly acceptedAffirmations = signal<string[]>([]);
  readonly perAuthorAffirmations = signal<Record<string, boolean>>({});

  // ── Template + taxonomies ───────────────────────────────────────────────

  readonly template = signal<FormTemplate | null>(null);
  readonly taxonomyTrees = signal<Record<string, TaxonomyTree>>({});

  readonly steps = computed<FormStep[]>(() => {
    const raw = this.template()?.uiSchema as Partial<FormUiSchema> | undefined;
    if (raw?.steps && raw.steps.length > 0) return raw.steps;
    return this.defaultSteps(this.template());
  });

  readonly currentStepIdx = signal(0);
  readonly currentStep = computed(() => this.steps()[this.currentStepIdx()]);
  readonly isLastStep = computed(() => this.currentStepIdx() === this.steps().length - 1);
  readonly stepIncludesAffirmations = computed(() =>
    this.steps().some((s) => s.fields.some((f) => f.kind === 'affirmations')),
  );

  private defaultSteps(tpl: FormTemplate | null): FormStep[] {
    const fields: FormStep['fields'] = [];
    for (const key of tpl?.requiredTaxonomyKeys ?? []) {
      fields.push({ kind: 'taxonomy', taxonomyKey: key });
    }
    fields.push({ kind: 'title' });
    const props = tpl?.schema?.properties ?? {};
    for (const [key, spec] of Object.entries(props)) {
      fields.push({ kind: 'text', property: key, label: spec?.title ?? key });
    }
    return [
      { id: 'basics', title: this.tr('submissions.wizard.step_basics'), fields },
      { id: 'authors', title: this.tr('submissions.wizard.step_authors'), fields: [{ kind: 'author-block' }] },
      { id: 'affirmations', title: this.tr('submissions.wizard.step_affirmations'), fields: [{ kind: 'affirmations' }] },
    ];
  }

  // ── Loaders ──────────────────────────────────────────────────────────────

  async loadTemplate(): Promise<void> {
    this.template.set(null);
    this.taxonomyTrees.set({});
    this.currentStepIdx.set(0);
    this.result.set(null);
    this.screenerInput.set(null);
    const slug = this.slug();
    if (!slug) return;
    try {
      const active = await this.formTemplatesApi.listActive(slug);
      const chosen = active.find((x) => x.key === 'free-paper') ?? active[0] ?? null;
      this.template.set(chosen);
      if (!chosen) return;
      const trees = await Promise.all(
        (chosen.requiredTaxonomyKeys ?? []).map(
          async (k) => [k, await this.taxonomiesApi.getTree(slug, k)] as const,
        ),
      );
      this.taxonomyTrees.set(Object.fromEntries(trees));
    } catch {
      this.template.set(null);
    }
  }

  // ── Field mutators (immutable signal updates) ─────────────────────────────

  setPayload(property: string, value: string): void {
    this.payload.update((m) => ({ ...m, [property]: value }));
  }
  setTaxonomy(key: string, value: string): void {
    this.selectedNodeByTaxonomy.update((m) => ({ ...m, [key]: value }));
  }
  onSlugChange(value: string): void {
    this.slug.set(value);
    this.loadTemplate(); // mirrors Vue's watch(slug)
  }

  // ── Validation per step ────────────────────────────────────────────────────

  validateStep(step: FormStep): string | null {
    for (const field of step.fields) {
      if (field.kind === 'title' && !this.title().trim()) {
        return this.tr('submissions.wizard.validation.title');
      }
      if (field.kind === 'taxonomy' && field.taxonomyKey) {
        if (!this.selectedNodeByTaxonomy()[field.taxonomyKey]) {
          const tree = this.taxonomyTrees()[field.taxonomyKey];
          return this.tr('submissions.validation.taxonomy', {
            name: tree?.taxonomy.displayName ?? field.taxonomyKey,
          });
        }
      }
      if (field.kind === 'text' && field.property) {
        const v = this.payload()[field.property] ?? '';
        if (v.length < 50) {
          return this.tr('submissions.wizard.validation.text_min', {
            label: field.label ?? field.property,
          });
        }
      }
      if (field.kind === 'author-block') {
        const model = this.authorModel();
        if (model.submitterAffiliations.length === 0) {
          return this.tr('submissions.validation.affiliation');
        }
        const p = model.presenter;
        const presenterStarted = p.givenName || p.familyName || p.email;
        if (presenterStarted) {
          if (!p.givenName || !p.familyName) {
            return this.tr('submissions.wizard.validation.presenter_name');
          }
          if (p.affiliations.length === 0) {
            return this.tr('submissions.validation.presenter_affiliation');
          }
        }
      }
    }
    return null;
  }

  next(): void {
    this.error.set(null);
    const err = this.validateStep(this.currentStep());
    if (err) {
      this.error.set(err);
      return;
    }
    this.currentStepIdx.update((i) => i + 1);
  }

  prev(): void {
    this.error.set(null);
    this.currentStepIdx.update((i) => Math.max(0, i - 1));
  }

  cancel(): void {
    this.location.back();
  }

  copyScreenerInput(): void {
    const s = this.screenerInput();
    if (s) void navigator.clipboard?.writeText(JSON.stringify(s));
  }

  // ── Finish / persist ───────────────────────────────────────────────────────

  private async ensureSelfPerson(): Promise<string> {
    const p = this.auth.profile()!;
    const [given, ...rest] = (p.name ?? p.preferredUsername ?? 'Author').split(' ');
    const family = rest.join(' ') || '—';
    const person = await this.peopleApi.create({ givenName: given, familyName: family, email: p.email });
    return person.id;
  }

  async finish(): Promise<void> {
    this.error.set(null);
    for (const step of this.steps()) {
      const err = this.validateStep(step);
      if (err) {
        this.error.set(err);
        return;
      }
    }
    this.saving.set(true);
    try {
      const submitterPersonId = await this.ensureSelfPerson();

      // 1) Create the submission draft.
      const formPayload: Record<string, unknown> = { ...this.payload() };
      const draft = await this.submissionsApi.create({
        conferenceSlug: this.slug(),
        formTemplateKey: this.template()?.key ?? 'free-paper',
        type: this.type(),
        title: this.title().trim(),
        formPayload,
        submitterPersonId,
        taxonomyNodeIds: Object.values(this.selectedNodeByTaxonomy()).filter(Boolean) as string[],
      });

      // 2) Replace authors. Submitter is always first; presenter + co-authors
      //    are created through the people API, then replaced in one shot.
      const model = this.authorModel();
      const authorsPayload: Array<{
        personId: string;
        role: 'SUBMITTER' | 'PRESENTER' | 'CO_AUTHOR';
        isCorresponding?: boolean;
        affiliationIds?: string[];
      }> = [
        {
          personId: submitterPersonId,
          role: 'SUBMITTER',
          isCorresponding: true,
          affiliationIds: model.submitterAffiliations.map((a) => a.id),
        },
      ];
      const presenter = model.presenter;
      if (presenter.givenName && presenter.familyName) {
        const p = await this.peopleApi.create({
          givenName: presenter.givenName,
          familyName: presenter.familyName,
          email: presenter.email || undefined,
        });
        authorsPayload.push({ personId: p.id, role: 'PRESENTER', affiliationIds: presenter.affiliations.map((a) => a.id) });
      }
      for (const c of model.coAuthors) {
        if (!c.givenName || !c.familyName) continue;
        const p = await this.peopleApi.create({
          givenName: c.givenName,
          familyName: c.familyName,
          email: c.email || undefined,
        });
        authorsPayload.push({ personId: p.id, role: 'CO_AUTHOR', affiliationIds: c.affiliations.map((a) => a.id) });
      }
      await this.submissionsApi.replaceAuthors(draft.id, { rowVersion: draft.rowVersion, authors: authorsPayload });

      // 3) Replace submission-level affirmation acceptance. Safe when empty.
      const perAuthorCalls: Array<{ affirmationId: string; personIds: string[] }> = [];
      if (this.stepIncludesAffirmations()) {
        await this.submissionsApi.replaceAffirmations(draft.id, this.acceptedAffirmations());

        // 3b) Per-author affirmations: submitter stamps on behalf of every author.
        const everyAuthorPersonId = authorsPayload.map((a) => a.personId);
        for (const [affirmationId, accepted] of Object.entries(this.perAuthorAffirmations())) {
          if (accepted) {
            await this.submissionsApi.replacePerAuthorAffirmation(draft.id, affirmationId, everyAuthorPersonId);
            perAuthorCalls.push({ affirmationId, personIds: everyAuthorPersonId });
          }
        }
      }

      // Production: this.router.navigate(['/submissions', draft.id, 'edit']);
      // Example: surface the assembled payload instead of navigating away.
      this.result.set({
        draftId: draft.id,
        create: {
          conferenceSlug: this.slug(),
          type: this.type(),
          title: this.title().trim(),
          formPayload,
          taxonomyNodeIds: Object.values(this.selectedNodeByTaxonomy()).filter(Boolean),
        },
        authors: authorsPayload,
        affirmations: this.acceptedAffirmations(),
        perAuthorAffirmations: perAuthorCalls,
      });

      // Screener-ready projection: {id, title, abstract} in a JSON array. The
      // abstract is the `abstractText` field, or all text fields joined.
      const abstract = this.payload()['abstractText'] ?? Object.values(this.payload()).join('\n\n');
      this.screenerInput.set([{ id: draft.id, title: this.title().trim(), abstract }]);
    } catch (e) {
      const p = problemOf(e);
      this.error.set(p?.detail ?? this.tr('submissions.error.save'));
    } finally {
      this.saving.set(false);
    }
  }

  // ── Lifecycle ────────────────────────────────────────────────────────────

  async ngOnInit(): Promise<void> {
    this.slug.set(this.route.snapshot.queryParamMap.get('conference') ?? '');
    const list = await this.conferencesApi.list();
    this.conferences.set(list);
    if (!this.slug() && list.length > 0) this.slug.set(list[0].slug);
    await this.loadTemplate();
  }

  // ── i18n shim ──────────────────────────────────────────────────────────────
  // Production: inject TranslateService and use `translate.instant(key, params)`
  // (and the `| translate` pipe in the template). Local map keeps the example
  // self-contained and readable.
  private static readonly LABELS: Record<string, string> = {
    'submissions.new': 'New submission (example)',
    'submissions.conference': 'Conference',
    'submissions.type': 'Type',
    'submissions.wizard.step_basics': 'Basics',
    'submissions.wizard.step_authors': 'Authors',
    'submissions.wizard.step_affirmations': 'Affirmations',
    'submissions.wizard.validation.title': 'A title is required.',
    'submissions.validation.taxonomy': 'Please choose a {name}.',
    'submissions.wizard.validation.text_min': '{label} must be at least 50 characters.',
    'submissions.validation.affiliation': 'Add at least one affiliation for the submitter.',
    'submissions.wizard.validation.presenter_name': 'Presenter needs both a given and family name.',
    'submissions.validation.presenter_affiliation': 'Add at least one affiliation for the presenter.',
    'submissions.abstract_placeholder': 'Describe the work…',
    'submissions.error.save': 'Could not save the submission.',
    'submissions.wizard.back': 'Back',
    'submissions.wizard.next': 'Next',
    'submissions.save_draft': 'Save draft',
    'common.cancel': 'Cancel',
    'common.saving': 'Saving…',
  };

  tr(key: string, params?: Record<string, unknown>): string {
    let s = ExampleSubmissionComponent.LABELS[key] ?? key;
    if (params) for (const [k, v] of Object.entries(params)) s = s.replace(`{${k}}`, String(v));
    return s;
  }
}
