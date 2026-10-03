import json
with open('project_24.json', 'r') as f:
    data = json.load(f)

sources = {}
severities = {}
for finding in data.get('findings', []):
    source = finding.get('source', 'UNKNOWN')
    severity = finding.get('severity', 'UNKNOWN')
    sources[source] = sources.get(source, 0) + 1
    severities[severity] = severities.get(severity, 0) + 1

print("Sources:", sources)
print("Severities:", severities)
