# -*- coding: utf-8 -*-
"""
@file    logica.py
@module  src.Web.negocio.logica
@brief   DAO SQLite para la tabla 'mediciones'.
@author  Marc Vilagrosa Caturla
"""

from pathlib import Path
import sqlite3

# Ruta a la BD: .../src/Web/bbdd/mediciones.db
DB_PATH = Path(__file__).resolve().parents[1] / "bbdd" / "mediciones.db"

def insertar_medicion(valor: float) -> bool:
    """
    Inserta una nueva medición en la tabla 'mediciones'.

    Inserta (valor, CURRENT_TIMESTAMP). 'id' lo autogestiona SQLite.

    Args:
        valor (float): dato numérico a guardar.

    Returns:
        bool: True si se insertó exactamente 1 fila; False en otro caso.
    """
    # Conexión a la BD
    con = sqlite3.connect(DB_PATH.as_posix())
    cur = con.cursor()
    cur.execute(
        "INSERT INTO mediciones (valor, fecha) VALUES (?, CURRENT_TIMESTAMP)",
        (valor,),
    )
    con.commit()
    ok = cur.rowcount == 1
    con.close()
    return ok


def get_ultima_medicion():
    """
    Recupera la última medición (por id descendente).

    Returns:
        tuple | None: (id, valor, fecha) si hay filas; None si la tabla está vacía.
    """
    con = sqlite3.connect(DB_PATH.as_posix())
    cur = con.cursor()

    # Seleccionamos la fila con mayor id (la más reciente insertada)
    cur.execute(
        "SELECT id, valor, fecha FROM mediciones ORDER BY id DESC LIMIT 1"
    )
    row = cur.fetchone()  # Lee esa única fila (o None si no hay filas)

    con.close()
    return row


def get_mediciones(limit: int = 50):
    """
    Devuelve la lista mediciones ordenadas de más reciente a más antigua.

    Args:
        limit (int, opcional): número máximo de filas a devolver. Por defecto 50.

    Returns:
        list[tuple]: lista de tuplas (id, valor, fecha), tamaño <= limit.
    """
    con = sqlite3.connect(DB_PATH.as_posix())
    cur = con.cursor()

    # LIMIT parametrizado para evitar concatenar cadenas en SQL
    cur.execute(
        "SELECT id, valor, fecha FROM mediciones ORDER BY id DESC LIMIT ?",
        (limit,),
    )
    rows = cur.fetchall()  # Lee todas las filas resultantes (lista vacía si no hay filas)

    con.close()
    return rows
