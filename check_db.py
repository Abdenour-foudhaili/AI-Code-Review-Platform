import mysql.connector

# Connect to the database
conn = mysql.connector.connect(
    host="localhost",
    user="root",
    password="",
    database="aicodereview"
)
cursor = conn.cursor(dictionary=True)

cursor.execute("SELECT id, source, rule_id, file, line_number FROM review_findings WHERE project_id = 24")
rows = cursor.fetchall()
sources = {}
for row in rows:
    source = row['source']
    sources[source] = sources.get(source, 0) + 1

print("Sources in DB:", sources)
