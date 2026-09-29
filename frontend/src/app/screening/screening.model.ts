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
}
