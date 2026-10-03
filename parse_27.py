import json
with open('project_27.json', 'r') as f:
    data = json.load(f)
findings = data.get('findings', [])
sources = {}
for finding in findings:
    source = finding.get('source', 'UNKNOWN')
    sources[source] = sources.get(source, 0) + 1
print("Total:", len(findings))
print("Sources:", sources)
print("Files:", list(set(f.get('file') for f in findings)))
