# src/Web/negocio/medicion_service.py
from pathlib import Path
import sqlite3

DB_PATH = Path(__file__).resolve().parents[1] / "bbdd" / "mediciones.db"

def insertar_medicion(valor: float) -> bool:
    con = sqlite3.connect(DB_PATH.as_posix())
    cur = con.cursor()
    cur.execute("INSERT INTO mediciones (valor, fecha) VALUES (?, CURRENT_TIMESTAMP)", (valor,))
    con.commit()
    ok = cur.rowcount == 1
    con.close()
    return ok

def get_ultima_medicion():
    con = sqlite3.connect(DB_PATH.as_posix())
    cur = con.cursor()
    cur.execute("SELECT id, valor, fecha FROM mediciones ORDER BY id DESC LIMIT 1")
    row = cur.fetchone()
    con.close()
    return row

def get_mediciones(limit: int = 50):
    con = sqlite3.connect(DB_PATH.as_posix())
    cur = con.cursor()
    cur.execute("SELECT id, valor, fecha FROM mediciones ORDER BY id DESC LIMIT ?", (limit,))
    rows = cur.fetchall()
    con.close()
    return rows
