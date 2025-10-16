# -*- coding: utf-8 -*-
"""
@file    logica_test.py
@module  src.test.logica_test
@brief   Test mínimo de la lógica de negocio: insertar_medicion() → get_ultima_medicion().
"""

import sys, os, sqlite3
import pytest

# --------------------------------------------------------------
# PYTHONPATH: añadimos la raíz del repo para importar `src.Web.negocio.logica`
# --------------------------------------------------------------
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))

from src.Web.negocio import logica as logic


def test_insertar_y_leer_ultima(tmp_path, monkeypatch):
    """
    Test muy simple:
      1) Crea una BD temporal y su tabla `mediciones`.
      2) Inserta una medición (valor=235).
      3) Lee la última y comprueba el valor.

    Nota: este test NO toca tu BD real. Redirige la lógica a un .db temporal.
    Si en tu módulo `logica` el path de BD se llama distinto a `DB_PATH`
    (p.ej. `RUTA_BD`), cambia el nombre en el monkeypatch.
    """
    # 1) BD temporal + tabla
    db = tmp_path / "logic_test.db"
    # db ya es un pathlib.Path (tmp_path / "logic_test.db")
    monkeypatch.setattr(logic, "DB_PATH", db, raising=False)       # ✅ pásalo como Path


    con = sqlite3.connect(str(db))
    con.execute("""
        CREATE TABLE IF NOT EXISTS mediciones (
            id    INTEGER PRIMARY KEY AUTOINCREMENT,
            valor REAL NOT NULL,
            fecha TEXT  NOT NULL DEFAULT (CURRENT_TIMESTAMP)
        );
    """)
    con.commit()
    con.close()

    # 2) Inserta
    ok = logic.insertar_medicion(235)
    assert ok is True

    # 3) Lee última
    row = logic.get_ultima_medicion()
    assert row is not None
    id_, valor, fecha = row

    assert isinstance(id_, int)
    assert valor == 235
    assert isinstance(fecha, (str,))  # la lógica suele devolver fecha en string (SQLite)
