import { ReviewHistoryItem, ReviewSummary, ReviewResult, Finding } from '../models/review.model';

export const MOCK_REVIEW_SUMMARY: ReviewSummary = {
  totalReviews: 128,
  issuesDetected: 432,
  securityIssues: 12,
  averageCodeQuality: 'B+'
};

export const MOCK_HISTORY: ReviewHistoryItem[] = [
  { id: '1', project: 'Payment Service', language: 'Java', date: 'Today, 14:30', issuesCount: 12, securityCount: 2, status: 'Completed' },
  { id: '2', project: 'User Profile API', language: 'TypeScript', date: 'Yesterday', issuesCount: 5, securityCount: 0, status: 'Completed' },
  { id: '3', project: 'Authentication Module', language: 'Go', date: 'Oct 24, 2023', issuesCount: 0, securityCount: 0, status: 'In Progress' },
  { id: '4', project: 'Legacy Billing App', language: 'Java', date: 'Oct 20, 2023', issuesCount: 45, securityCount: 8, status: 'Completed' },
  { id: '5', project: 'Frontend Dashboard', language: 'TypeScript', date: 'Oct 18, 2023', issuesCount: 3, securityCount: 0, status: 'Failed' },
];

export const MOCK_FINDINGS: Finding[] = [
  {
    id: 'f1',
    category: 'Security Vulnerability',
    severity: 'High',
    file: 'UserController.java',
    line: 42,
    problem: 'SQL Injection Risk',
    explanation: 'The user input is directly concatenated into the SQL query, which allows attackers to execute arbitrary SQL commands.',
    recommendation: 'Use parameterized queries or prepared statements instead of string concatenation.',
    originalCode: 'String query = "SELECT * FROM users WHERE username = \'" + request.getParameter("username") + "\'";\nResultSet rs = stmt.executeQuery(query);',
    suggestedCode: 'String query = "SELECT * FROM users WHERE username = ?";\nPreparedStatement pstmt = conn.prepareStatement(query);\npstmt.setString(1, request.getParameter("username"));\nResultSet rs = pstmt.executeQuery();'
  },
  {
    id: 'f2',
    category: 'Code Smell',
    severity: 'Medium',
    file: 'PaymentProcessor.java',
    line: 115,
    problem: 'Complex Method',
    explanation: 'The processPayment method has a high cyclomatic complexity and is difficult to maintain.',
    recommendation: 'Extract smaller methods for validation, calculation, and API communication.',
    originalCode: 'public void processPayment(...) {\n  // 150 lines of complex nested logic\n}',
    suggestedCode: 'public void processPayment(...) {\n  validatePaymentDetails();\n  calculateTotal();\n  executeTransaction();\n}'
  },
  {
    id: 'f3',
    category: 'Bug Risk',
    severity: 'Critical',
    file: 'AuthService.ts',
    line: 28,
    problem: 'Null Pointer Exception',
    explanation: 'The user object is accessed before checking if it is null or undefined.',
    recommendation: 'Add a null check or use optional chaining before accessing properties.',
    originalCode: 'const userId = response.data.user.id;\n// Will crash if user is undefined',
    suggestedCode: 'const userId = response.data.user?.id;\nif (!userId) return handleError();'
  },
  {
    id: 'f4',
    category: 'Best Practice',
    severity: 'Low',
    file: 'utils.js',
    line: 12,
    problem: 'Use const instead of let',
    explanation: 'The variable \'config\' is never reassigned.',
    recommendation: 'Use const to prevent accidental reassignment.',
    originalCode: 'let config = loadConfig();',
    suggestedCode: 'const config = loadConfig();'
  }
];

export const MOCK_REVIEW_RESULT: ReviewResult = {
  id: 'rev-123',
  summary: {
    critical: 1,
    high: 1,
    medium: 1,
    low: 1
  },
  findings: MOCK_FINDINGS
};
