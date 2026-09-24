export interface ProjectFileResponse {
  id: number;
  relativePath: string;
  language: string;
  analysisStatus: string;
}

export interface ProjectResponse {
  id: number;
  name: string;
  description: string;
  status: string;
  detectedLanguages: string;
  createdAt: string;
  updatedAt: string;
  qualityScore: string;
  filesCount: number;
}

export interface ProjectDetailsResponse extends ProjectResponse {
  findings: any[]; // reuse finding
  files: ProjectFileResponse[];
}
