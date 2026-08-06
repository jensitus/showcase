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
}
