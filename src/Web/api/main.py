# -*- coding: utf-8 -*-
"""
@file    main.py
@module  src.Web.api.main
@brief   API REST (FastAPI + SQLite) + web estática en /ux.
@author  Marc Vilagrosa Caturla
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import RedirectResponse
from pydantic import BaseModel, Field
from datetime import datetime, timezone
from pathlib import Path


# Capa de negocio (acceso BD) separada de los controladores HTTP
from src.Web.negocio.logica import (
    insertar_medicion,
    get_ultima_medicion,
    get_mediciones,
)

# ------------------ App ------------------
# Metadatos visibles en /docs y para organización
app = FastAPI(
    title="API Mediciones (Sprint 0)",
    version="v1",
    description="API REST (FastAPI + SQLite) + web estática en /ux.",
)

# ------------------ CORS ------------------
# Permite a la web levantada en :5500 (Live Server/http.server) llamar a esta API en :8000
# Afecta solo a navegadores (fetch/XHR).
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://127.0.0.1:5500"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ------------- Web estática --------------
# Sirve /ux con la interfaz estática (HTML/JS/CSS). Raíz / redirige a /ux/
STATIC_DIR = Path(__file__).resolve().parents[1] / "ux"  # .../src/Web/ux
app.mount("/ux", StaticFiles(directory=STATIC_DIR, html=True), name="ux")

@app.get("/", include_in_schema=False)
def root():
    """Redirección simple a la interfaz web."""
    return RedirectResponse(url="/ux/")

# ---------------- Modelos ----------------
# Validación de entrada/salida con Pydantic (contratos claros)
class MedicionIn(BaseModel):
    valor: float  

class MedicionOut(BaseModel):
    id: int
    valor: float
    fecha: datetime

def _parse_fecha(fecha_str: str) -> datetime:
    """
    Convierte la fecha (SQLite) a datetime *aware* en UTC.
    Así el frontend la interpreta con zona y la muestra en local correctamente.
    """
    s = str(fecha_str).strip().replace(" ", "T")
    dt = None
    # Intento 1: ISO sin zona
    try:
        dt = datetime.fromisoformat(s)
    except Exception:
        pass
    # Intento 2: formatos comunes sin 'T'
    if dt is None:
        for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S"):
            try:
                dt = datetime.strptime(str(fecha_str), fmt)
                break
            except Exception:
                continue
    if dt is None:
        raise ValueError(f"No se pudo parsear fecha: {fecha_str}")

    # Asumimos que lo guardado por SQLite es UTC → marcamos tz
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    else:
        dt = dt.astimezone(timezone.utc)
    return dt


# --------------- Endpoints ---------------
# Controladores HTTP; no llevan SQL directo: delegan en la capa de negocio.

@app.post("/api/v1/mediciones", response_model=MedicionOut, status_code=201, tags=["mediciones"])
def crear_medicion(body: MedicionIn):
    """Inserta una medición y devuelve la fila creada {id, valor, fecha}."""
    ok = insertar_medicion(body.valor)
    if not ok:
        raise HTTPException(status_code=500, detail="No se pudo insertar la medición")
    row = get_ultima_medicion()
    if not row:
        raise HTTPException(status_code=500, detail="No se pudo recuperar la medición insertada")
    id_, valor, fecha_str = row
    return {"id": id_, "valor": valor, "fecha": _parse_fecha(fecha_str)}

@app.get("/api/v1/mediciones/ultima", response_model=MedicionOut, tags=["mediciones"])
def ultima():
    """Devuelve la última medición (por id DESC)."""
    row = get_ultima_medicion()
    if not row:
        raise HTTPException(status_code=404, detail="No hay mediciones")
    id_, valor, fecha_str = row
    return {"id": id_, "valor": valor, "fecha": _parse_fecha(fecha_str)}

@app.get("/api/v1/mediciones", tags=["mediciones"])
def listar_mediciones(limit: int = 50):
    """Lista de mediciones ordenadas DESC por id. Parámetro ?limit para acotar el número."""
    rows = get_mediciones(limit)
    out = []
    for id_, valor, fecha in rows:  # filas como tuplas (id, valor, fecha)
        out.append({"id": int(id_), "valor": float(valor), "fecha": _parse_fecha(str(fecha))})
    return out

