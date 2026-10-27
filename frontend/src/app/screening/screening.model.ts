export interface BatchListItem {
  batchId: string;
  total: number;
  flagged: number;
}

export interface FlaggedRow {
  submissionId: string;
  verdict: string;
  noveltyScore: string;
  title: string;
  matchedPriorTitle: string;
  matchedYear: string;
  similarity: string;
  rationale: string;
  /** The larger model's "adds anything new?" answer: 'yes' | 'partly' | 'no', '' if not asked. */
  llmAddsNew: string;
  /** Its one sentence: what is new, or why nothing is. */
  llmSays: string;
}

export interface BatchDetail {
  batchId: string;
  total: number;
  flagged: number;
  verdictCounts: Record<string, number>;
  rows: FlaggedRow[];
  /**
   * Year cutoff the batch was screened under, or null when there was none. Derived by
   * the API from the reports themselves. Changes what every other number on the page
   * means, so it is displayed rather than assumed.
   */
  beforeYear: number | null;
  /** The larger model's answers (yes / partly / no -> count); empty when it was not run. */
  llmCounts: Record<string, number>;
}
