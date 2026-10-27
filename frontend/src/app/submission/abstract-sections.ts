/**
 * The sections of an abstract, in order — mirrors the backend's AbstractSection (which
 * validates again; keep the two in step). Labels are translated via SUBMISSION.SECTION_<shortname>.
 */
export interface AbstractSectionDef {
  shortname: string;
  required: boolean;
  maxLength: number;
  rows: number;
}

export const MAX_REQUIRED = 2500;
export const MAX_OPTIONAL = 1500;
export const MAX_TOTAL = 7000;

export const ABSTRACT_SECTIONS: AbstractSectionDef[] = [
  {shortname: 'PURPOSE', required: true, maxLength: MAX_REQUIRED, rows: 3},
  {shortname: 'METHODS', required: true, maxLength: MAX_REQUIRED, rows: 5},
  {shortname: 'RESULTS', required: true, maxLength: MAX_REQUIRED, rows: 5},
  {shortname: 'CONCLUSION', required: true, maxLength: MAX_REQUIRED, rows: 3},
  {shortname: 'LIMITATIONS', required: false, maxLength: MAX_OPTIONAL, rows: 2},
  {shortname: 'FUNDING', required: false, maxLength: MAX_OPTIONAL, rows: 2},
  {shortname: 'ETHICS', required: false, maxLength: MAX_OPTIONAL, rows: 2},
];

export interface AbstractSection {
  shortname: string;
  /** The label; set by the backend in responses. */
  name?: string;
  content: string;
}
