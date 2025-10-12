# src/Web/api/main.py
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import RedirectResponse
from pydantic import BaseModel, Field
from datetime import datetime
from pathlib import Path
import sqlite3

# ===== App =====
app = FastAPI(
    title="API Mediciones (Sprint 0)",
    version="v1",
    description="API REST (FastAPI + SQLite) + web estática en /ux."
)

# CORS para servir la web desde :5500
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5500",
        "http://127.0.0.1:5500",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ===== WEB ESTÁTICA (opcional) =====
# También puedes abrir la web en http://127.0.0.1:8000/ux
STATIC_DIR = Path(__file__).resolve().parents[1] / "ux"
app.mount("/ux", StaticFiles(directory=STATIC_DIR, html=True), name="ux")

@app.get("/", include_in_schema=False)
def root():
    return RedirectResponse(url="/ux/")

# ===== Modelos =====
class MedicionIn(BaseModel):
    valor: float = Field(..., examples=[22.53])

class MedicionOut(BaseModel):
    id: int
    valor: float
    fecha: datetime

def _parse_fecha(fecha_str: str) -> datetime:
    try:
        return datetime.fromisoformat(str(fecha_str).replace(" ", "T"))
    except Exception:
        for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S"):
            try:
                return datetime.strptime(str(fecha_str), fmt)
            except Exception:
                pass
        raise

# ===== Acceso SQLite (fallback sencillo) =====
DB_PATH = Path(__file__).resolve().parents[1] / "bbdd" / "mediciones.db"

def _sql_select_many(limit: int = 50):
    con = sqlite3.connect(DB_PATH.as_posix())
    cur = con.cursor()
    cur.execute("SELECT id, valor, fecha FROM mediciones ORDER BY id DESC LIMIT ?", (limit,))
    rows = cur.fetchall()
    con.close()
    return rows

def _sql_insert(valor: float) -> bool:
    con = sqlite3.connect(DB_PATH.as_posix())
    cur = con.cursor()
    cur.execute("INSERT INTO mediciones (valor, fecha) VALUES (?, CURRENT_TIMESTAMP)", (valor,))
    con.commit()
    ok = cur.rowcount == 1
    con.close()
    return ok

def _sql_select_last():
    con = sqlite3.connect(DB_PATH.as_posix())
    cur = con.cursor()
    cur.execute("SELECT id, valor, fecha FROM mediciones ORDER BY id DESC LIMIT 1")
    row = cur.fetchone()
    con.close()
    return row

# ===== Endpoints =====
@app.get("/api/v1/health")
def health():
    return {"status": "ok", "time": datetime.now().isoformat()}

@app.post("/api/v1/mediciones", response_model=MedicionOut, status_code=201, tags=["mediciones"])
def crear_medicion(body: MedicionIn):
    # Usa tu servicio si lo tienes; fallback directo a SQLite
    try:
        from src.Web.negocio.medicion_service import insertar_medicion, get_ultima_medicion  # type: ignore
        ok = insertar_medicion(body.valor)
        row = get_ultima_medicion() if ok else None
    except Exception:
        ok = _sql_insert(body.valor)
        row = _sql_select_last() if ok else None

    if not ok:
        raise HTTPException(status_code=500, detail="No se pudo insertar la medición")
    if not row:
        raise HTTPException(status_code=500, detail="No se pudo recuperar la medición insertada")

    id_, valor, fecha_str = row
    return {"id": id_, "valor": valor, "fecha": _parse_fecha(fecha_str)}

@app.get("/api/v1/mediciones/ultima", response_model=MedicionOut, tags=["mediciones"])
def ultima():
    try:
        from src.Web.negocio.medicion_service import get_ultima_medicion  # type: ignore
        row = get_ultima_medicion()
    except Exception:
        row = _sql_select_last()

    if not row:
        raise HTTPException(status_code=404, detail="No hay mediciones")
    id_, valor, fecha_str = row
    return {"id": id_, "valor": valor, "fecha": _parse_fecha(fecha_str)}

@app.get("/api/v1/mediciones", tags=["mediciones"])
def listar_mediciones(limit: int = 50):
    """
    Devuelve lista: [{id, valor, fecha}, ...] ordenadas desc por id.
    """
    try:
        from src.Web.negocio.medicion_service import get_mediciones  # type: ignore
        rows = get_mediciones(limit)
    except Exception:
        rows = _sql_select_many(limit)

    out = []
    for r in rows:
        if isinstance(r, (list, tuple)):
            id_, valor, fecha = r[0], r[1], r[2]
        elif isinstance(r, dict):
            id_, valor, fecha = r.get("id"), r.get("valor"), r.get("fecha")
        else:
            id_, valor, fecha = getattr(r, "id"), getattr(r, "valor"), getattr(r, "fecha")
        out.append({"id": int(id_), "valor": float(valor), "fecha": _parse_fecha(str(fecha))})
    return out
