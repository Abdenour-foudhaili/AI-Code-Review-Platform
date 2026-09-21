export interface ReviewSummary {
  totalReviews: number;
  issuesDetected: number;
  securityIssues: number;
  averageCodeQuality: string;
}

export interface ReviewHistoryItem {
  id: string;
  project: string;
  language: string;
  date: string;
  issuesCount: number;
  securityCount: number;
  status: 'Completed' | 'In Progress' | 'Failed';
}

export type Severity = 'Critical' | 'High' | 'Medium' | 'Low' | 'Info';

export interface Finding {
  id: string;
  category: string;
  severity: Severity;
  file: string;
  line: number;
  problem: string;
  explanation: string;
  recommendation: string;
  originalCode?: string;
  suggestedCode?: string;
}

export interface ReviewResult {
  id: string;
  summary: {
    critical: number;
    high: number;
    medium: number;
    low: number;
  };
  findings: Finding[];
}
