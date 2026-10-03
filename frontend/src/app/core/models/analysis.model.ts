export interface GithubStatus {
  configured: boolean;
  connected: boolean;
  login: string | null;
  avatarUrl: string | null;
  connectedAt: string | null;
}

export interface GithubRepo {
  id: number;
  name: string;
  fullName: string;
  owner: string;
  description: string;
  language: string;
  stars: number;
  isPrivate: boolean;
  updatedAt: string;
  defaultBranch: string;
  htmlUrl: string;
}

export interface AnalysisSummary {
  id: number;
  name: string;
  sourceType: string;
  repositoryUrl: string | null;
  branch: string | null;
  commitHash: string | null;
  status: string;
  stage: string | null;
  qualityScore: string | null;
  createdAt: string;
  completedAt: string | null;
  findingsCount: number;
}

export interface StageInfo {
  key: string;
  label: string;
  state: 'done' | 'active' | 'pending';
}

export interface AnalysisStatus {
  id: number;
  status: string;
  stage: string;
  message: string;
  progress: number;
  stages: StageInfo[];
  error: string | null;
}

export interface AnalysisDetails {
  summary: AnalysisSummary;
  summaryText: string | null;
  technologies: string[];
  warnings: string[];
  bySeverity: Record<string, number>;
  byCategory: Record<string, number>;
  bySource: Record<string, number>;
}

export interface Finding {
  id: number;
  category: string;
  severity: string;
  title: string;
  description: string;
  file: string;
  lineNumber: number | null;
  recommendation: string;
  fixedCode: string | null;
  source: string;
  ruleId: string | null;
  evidence: string | null;
}

export interface Diagram {
  type: string;
  title: string;
  mermaid: string;
}

export interface CodeContext {
  file: string;
  line: number;
  startLine: number;
  language: string;
  lines: string[];
}

export interface ReviewRule {
  id: number;
  name: string;
  description: string;
  ruleText: string;
  enabled: boolean;
  createdAt: string;
}

export interface DashboardData {
  repositories: number;
  analyses: number;
  completedAnalyses: number;
  singleFileReviews: number;
  criticalFindings: number;
  securityFindings: number;
  recentAnalyses: { id: number; name: string; sourceType: string; status: string; stage: string; qualityScore: string | null; createdAt: string }[];
  lastAnalysis: { id: number; name: string; summary: string; qualityScore: string | null; completedAt: string | null } | null;
  architectureRoles: Record<string, number>;
  architectureStyles: string[];
  technologies: string[];
  github: GithubStatus;
}
