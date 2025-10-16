# Sprint 0 — Proyecto **Biometría**

## Descripción del proyecto
Es un sistema que integra varias tecnologías.  
Un Arduino actúa como emisor BLE (beacon), enviando información de forma periódica. Una aplicación Android recibe esas tramas BLE, las procesa y las reenvía mediante HTTP hacia una API REST La API, desarrollada con FastAPI, se encarga de almacenar los datos en una base de datos SQLite.  
Además, se incluye una interfaz web sencilla (HTML + JavaScript) que consulta la API y permite visualizar las mediciones registradas.  

De esta forma, el sistema combina captura de datos (Arduino), recepción y reenvío (Android), almacenamiento y exposición (API + BD) y visualización (Web), todo dentro de una arquitectura clara y modular.  

Flujo: **Arduino → Android (BLE) → API (HTTP) → BD (SQLite) → Web (fetch)**.

---

## Estructura de carpetas
```
.
├─ src/
│  ├─ android/     # Aplicación Android: escaneo BLE y cliente HTTP
│  ├─ arduino/     # Código Arduino para emitir el beacon BLE
│  └─ web/         # API REST (FastAPI), BD SQLite y vista web (HTML+JS)
├─ doc/            # Documentación del proyecto (diagramas, notas, etc.)
└─ test/           # Preparado para pruebas futuras
```

---

## Despliegue local

> **Requisitos:** Python 3.10+ y `pip`. Ejecuta los comandos **desde la raíz del repositorio**. Mantén **dos terminales** abiertas: una para la API y otra para la web.

### 1 Instalar dependencias

>python -m pip install -r requirements.txt

### 2 Arrancar la API (FastAPI)
> python -m uvicorn src.Web.api.main:app --reload --host 0.0.0.0 --port 8000

### 3 Deplegar web estática

> python -m http.server 5500 --bind 0.0.0.0 --directory src/Web/ux

### 4 Enlaces Web

Docs: http://127.0.0.1:8000/docs
Web: http://127.0.0.1:5500/index.html 

## Ejecutar tests (pytest)

Ejecuta siempre los tests desde la raíz del proyecto.

### Todos los tests

> pytest -q

### Un archivo concreto

> pytest -q src/web/test/api_test.py
> pytest -q src/web/test/logica_test.py

