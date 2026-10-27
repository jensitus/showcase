import {AbstractSection} from './abstract-sections';

export interface Submission {
  id: string;
  title: string;
  authors: string;
  /** The whole abstract as one text (generated from the sections when there are any). */
  abstractText: string;
  /** In order; empty for submissions from before the sectioned form. */
  sections: AbstractSection[];
  topic: string;
  submitterEmail: string;
  submissionNumber: number;
  state: string;
  reviewerNotes?: string;
  createdBy?: string;
  submittedAt: string;
  updatedAt?: string;
}
