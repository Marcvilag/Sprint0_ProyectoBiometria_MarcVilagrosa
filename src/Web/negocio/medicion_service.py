import sqlite3
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(BASE_DIR, "../bbdd/mediciones.db")

def insertar_medicion(valor: float):
    """Inserta una nueva medición en la BBDD"""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS Medicion (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        valor REAL NOT NULL,
        fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)
    cursor.execute("INSERT INTO Medicion (valor) VALUES (?)", (valor,))
    conn.commit()
    conn.close()
    return True

def get_ultima_medicion():
    """Devuelve la última medición registrada"""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT id, valor, fecha FROM Medicion ORDER BY fecha DESC LIMIT 1")
    row = cursor.fetchone()
    conn.close()
    return row  # (id, valor, fecha) o None
