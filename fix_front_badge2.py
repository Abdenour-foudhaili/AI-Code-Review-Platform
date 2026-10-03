import os

path = 'frontend/src/app/features/projects/project-details/project-details.ts'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

target = """                  <div class="flex items-center gap-sm">
                    <span class="badge"
                          [class.badge-danger]="finding.severity === 'CRITICAL' || finding.severity === 'HIGH'\""""

replacement = """                  <div class="flex items-center gap-sm">
                    <span class="badge" *ngIf="finding.source"
                          [class.badge-info]="finding.source === 'LLM'"
                          [class.badge-warning]="finding.source === 'RUFF'"
                          [class.badge-danger]="finding.source === 'BANDIT' || finding.source === 'GITLEAKS'"
                          style="border: 1px solid currentColor;">
                      {{ finding.source }}
                    </span>
                    <span class="badge"
                          [class.badge-danger]="finding.severity === 'CRITICAL' || finding.severity === 'HIGH'\""""

text = text.replace(target, replacement)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
