export interface Submission {
  id: string;
  title: string;
  authors: string;
  abstractText: string;
  topic: string;
  submitterEmail: string;
  submissionNumber: number;
  state: string;
  reviewerNotes?: string;
  createdBy?: string;
  submittedAt: string;
  updatedAt?: string;
}
