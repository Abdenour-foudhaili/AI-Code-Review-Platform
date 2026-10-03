import sqlite3


def get_connection():
    return sqlite3.connect("app.db")


def get_user(username):
    conn = get_connection()
    cursor = conn.cursor()
    # SQL injection : concaténation directe de l'entrée utilisateur
    query = "SELECT * FROM users WHERE username = '" + username + "'"
    cursor.execute(query)
    return cursor.fetchall()


def login(username, password):
    conn = get_connection()
    cursor = conn.cursor()
    query = f"SELECT * FROM users WHERE username='{username}' AND password='{password}'"
    cursor.execute(query)
    return cursor.fetchone() is not None


def delete_user(user_id):
    conn = get_connection()
    conn.execute("DELETE FROM users WHERE id = %s" % user_id)
    conn.commit()
