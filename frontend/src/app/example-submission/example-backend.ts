/**
 * Self-contained mock backend + types for the EXAMPLE submission wizard.
 *
 * In the real app these come from `@/api/*` (Observable-based services), the
 * auth store, and `@/api/types`. Here they are in-memory, Promise-based fakes
 * so the wizard renders and "submits" at /example-submission without a backend.
 * Swap these injects for your real services to productionise.
 */
import { Injectable, signal } from '@angular/core';

// ── Types (mirror @/api/types) ───────────────────────────────────────────────

export type SubmissionType = 'FREE_PAPER' | 'INVITED' | 'LATE_BREAKING';

export interface Conference {
  id: string;
  slug: string;
  displayName: string;
}

export type FieldKind = 'title' | 'taxonomy' | 'text' | 'author-block' | 'affirmations';

export interface FieldSpec {
  kind: FieldKind;
  taxonomyKey?: string;
  property?: string;
  label?: string;
  rows?: number;
}

export interface FormStep {
  id: string;
  title: string;
  fields: FieldSpec[];
}

export interface FormUiSchema {
  steps: FormStep[];
}

export interface FormTemplate {
  key: string;
  uiSchema?: FormUiSchema;
  requiredTaxonomyKeys?: string[];
  schema?: { properties?: Record<string, { title?: string }> };
}

export interface TaxonomyNode {
  id: string;
  label: string;
}

export interface TaxonomyTree {
  taxonomy: { displayName: string };
  nodes: TaxonomyNode[];
}

export interface Affiliation {
  id: string;
  label: string;
}

export interface AuthorEntry {
  givenName: string;
  familyName: string;
  email: string;
  affiliations: Affiliation[];
}

export interface AuthorBlockModel {
  submitterAffiliations: Affiliation[];
  presenter: AuthorEntry;
  coAuthors: AuthorEntry[];
}

export interface Problem {
  detail?: string;
}

/** Mirrors `problemOf` from the real api client. */
export function problemOf(e: unknown): Problem | null {
  if (e && typeof e === 'object' && 'detail' in e) return e as Problem;
  return null;
}

/** Fixed affiliation catalogue the example author-block picks from. */
export const EXAMPLE_AFFILIATIONS: Affiliation[] = [
  { id: 'aff-1', label: 'General Hospital, Dept. of Radiology' },
  { id: 'aff-2', label: 'University Clinic, Cardiology' },
  { id: 'aff-3', label: 'Institute for Medical AI' },
];

// ── Mock services (mirror @/api/submissions + @/api/admin) ───────────────────

const rid = (p: string) => `${p}-${Math.random().toString(36).slice(2, 8)}`;
const delay = <T>(v: T) => new Promise<T>((r) => setTimeout(() => r(v), 150));

@Injectable({ providedIn: 'root' })
export class ConferencesApi {
  list(): Promise<Conference[]> {
    return delay([
      { id: 'c1', slug: 'demo-conf', displayName: 'Demo Conference 2026' },
      { id: 'c2', slug: 'spring-symposium', displayName: 'Spring Imaging Symposium' },
    ]);
  }
}

@Injectable({ providedIn: 'root' })
export class FormTemplatesApi {
  /**
   * Returns a free-paper template WITHOUT a uiSchema on purpose, so the wizard
   * exercises its default-step synthesis (taxonomy + title + text, then authors,
   * then affirmations). Give it a `uiSchema` to drive an explicit layout.
   */
  listActive(_slug: string): Promise<FormTemplate[]> {
    return delay([
      {
        key: 'free-paper',
        requiredTaxonomyKeys: ['topic'],
        schema: {
          properties: {
            abstractText: { title: 'Abstract' },
          },
        },
      },
    ]);
  }
}

@Injectable({ providedIn: 'root' })
export class TaxonomiesApi {
  getTree(_slug: string, key: string): Promise<TaxonomyTree> {
    return delay({
      taxonomy: { displayName: key === 'topic' ? 'Topic' : key },
      nodes: [
        { id: 't-ai', label: 'Artificial Intelligence' },
        { id: 't-imaging', label: 'Medical Imaging' },
        { id: 't-other', label: 'Other' },
      ],
    });
  }
}

@Injectable({ providedIn: 'root' })
export class PeopleApi {
  create(input: { givenName: string; familyName: string; email?: string }): Promise<{ id: string }> {
    return delay({ id: rid('person') });
  }
}

@Injectable({ providedIn: 'root' })
export class SubmissionsApi {
  create(_req: unknown): Promise<{ id: string; rowVersion: number }> {
    return delay({ id: rid('draft'), rowVersion: 1 });
  }
  replaceAuthors(_id: string, _body: unknown): Promise<void> {
    return delay(undefined);
  }
  replaceAffirmations(_id: string, _ids: string[]): Promise<void> {
    return delay(undefined);
  }
  replacePerAuthorAffirmation(_id: string, _affId: string, _personIds: string[]): Promise<void> {
    return delay(undefined);
  }
}

@Injectable({ providedIn: 'root' })
export class AuthStore {
  /** Mirrors a signal-based auth store: `auth.profile()`. */
  readonly profile = signal<{ name?: string; preferredUsername?: string; email: string } | null>({
    name: 'Alex Reviewer',
    email: 'alex.reviewer@example.org',
  });
}
