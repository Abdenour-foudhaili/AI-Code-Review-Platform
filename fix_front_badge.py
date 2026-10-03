import os

path = 'frontend/src/app/features/projects/project-details/project-details.ts'
with open(path, 'r', encoding='utf-8') as f:
    text = f.read()

# Insert badge
badge = """
                    <span class="badge" *ngIf="finding.source"
                          [class.badge-info]="finding.source === 'LLM'"
                          [class.badge-warning]="finding.source === 'RUFF'"
                          [class.badge-danger]="finding.source === 'BANDIT' || finding.source === 'GITLEAKS'"
                          style="border: 1px solid currentColor;">
                      {{ finding.source }}
                    </span>
                    <span class="badge\""""

text = text.replace('<span class="badge"', badge, 1)

with open(path, 'w', encoding='utf-8') as f:
    f.write(text)
