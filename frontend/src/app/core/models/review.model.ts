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

export enum ReviewStatus { PENDING = 'PENDING', ANALYZING = 'ANALYZING', COMPLETED = 'COMPLETED', FAILED = 'FAILED' }
export enum ReviewType { FULL_REVIEW = 'FULL_REVIEW', BUG_DETECTION = 'BUG_DETECTION', SECURITY = 'SECURITY', CODE_QUALITY = 'CODE_QUALITY' }
export enum FindingCategory { BUG = 'BUG', SECURITY = 'SECURITY', CODE_SMELL = 'CODE_SMELL', PERFORMANCE = 'PERFORMANCE', STYLE = 'STYLE', BEST_PRACTICE = 'BEST_PRACTICE' }
export enum FindingSeverity { CRITICAL = 'CRITICAL', HIGH = 'HIGH', MEDIUM = 'MEDIUM', LOW = 'LOW', INFO = 'INFO' }

export interface CreateReviewRequest {
  projectName: string;
  language: string;
  sourceCode: string;
  reviewType: ReviewType;
}
export interface ReviewResponse {
  id: number;
  projectName: string;
  language: string;
  reviewType: ReviewType;
  status: ReviewStatus;
  createdAt: string;
  completedAt: string;
}
export interface ReviewFindingResponse {
  id: number;
  category: FindingCategory;
  severity: FindingSeverity;
  title: string;
  description: string;
  lineNumber: number;
  recommendation: string;
  fixedCode: string;
}
export interface ReviewSummaryResponse {
  id: number;
  totalIssues: number;
  criticalIssues: number;
  highIssues: number;
  mediumIssues: number;
  lowIssues: number;
  qualityScore: string;
  summary: string;
}
export interface ReviewDetailsResponse extends ReviewResponse {
  sourceCode: string;
  summary: ReviewSummaryResponse;
  findings: ReviewFindingResponse[];
}

