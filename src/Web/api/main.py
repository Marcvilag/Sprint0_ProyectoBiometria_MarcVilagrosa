# src/Web/api/main.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from datetime import datetime
from pathlib import Path

from fastapi.staticfiles import StaticFiles
from fastapi.responses import RedirectResponse

from src.Web.negocio.medicion_service import insertar_medicion, get_ultima_medicion

app = FastAPI(
    title="API Mediciones (Sprint 0)",
    version="v1",
    description="API REST (FastAPI + SQLite) + web estática en /ux."
)

# ==== WEB ESTÁTICA: carpeta 'ux' ====
STATIC_DIR = Path(__file__).resolve().parents[1] / "ux" 
app.mount("/ux", StaticFiles(directory=STATIC_DIR, html=True), name="ux")

# Redirige la raíz a /ux
@app.get("/", include_in_schema=False)
def root():
    return RedirectResponse(url="/ux/")

# ==== MODELOS ====
class MedicionIn(BaseModel):
    valor: float = Field(..., examples=[22.53])

class MedicionOut(BaseModel):
    id: int
    valor: float
    fecha: datetime

def _parse_fecha(fecha_str: str) -> datetime:
    try:
        return datetime.fromisoformat(fecha_str.replace(" ", "T"))
    except Exception:
        from datetime import datetime as dt
        for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S"):
            try:
                return dt.strptime(fecha_str, fmt)
            except Exception:
                pass
        raise

# ==== ENDPOINTS ====
@app.get("/api/v1/health")
def health():
    return {"status": "ok"}

@app.post("/api/v1/mediciones", response_model=MedicionOut, status_code=201, tags=["mediciones"])
def crear_medicion(body: MedicionIn):
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
    row = get_ultima_medicion()
    if not row:
        raise HTTPException(status_code=404, detail="No hay mediciones")
    id_, valor, fecha_str = row
    return {"id": id_, "valor": valor, "fecha": _parse_fecha(fecha_str)}
