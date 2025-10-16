# -*- coding: utf-8 -*-
"""
@file    api_test.py
@module  src.test.api_test
@brief   Test mínimo de la API: POST /mediciones → GET /mediciones/ultima.
"""

import sys, os
from fastapi.testclient import TestClient

# --------------------------------------------------------------
# Ajuste de ruta: añadimos la raíz del repo al PYTHONPATH para
# poder importar `src.Web.api.main` desde `src/test/`.
# --------------------------------------------------------------
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..")))

from src.Web.api.main import app

# Cliente de pruebas para la app FastAPI
client = TestClient(app)


def test_post_then_get_ultima():
    """
    Test muy simple:
      1) Inserta una medición con un valor conocido (POST /api/v1/mediciones).
      2) Recupera la última medición (GET /api/v1/mediciones/ultima).
      3) Verifica que el valor coincide y que los códigos HTTP son correctos.

    Nota: este test escribe en la BD real (añade 1 fila). Si no quieres
    tocar tu BD en el futuro, puedes mockear la capa de negocio o usar
    una BD temporal para el entorno de CI.
    """
    valor = 235

    # 1) Crear (POST)
    r_post = client.post("/api/v1/mediciones", json={"valor": valor})
    assert r_post.status_code == 201
    j_post = r_post.json()
    assert "id" in j_post and "valor" in j_post

    # 2) Leer última (GET)
    r_get = client.get("/api/v1/mediciones/ultima")
    assert r_get.status_code == 200
    j_get = r_get.json()

    # 3) Comprobaciones esenciales
    assert j_get["id"] == j_post["id"]
    assert j_get["valor"] == valor
