# -*- coding: utf-8 -*-
"""
@file init_db.py
@author Marc Vilagrosa
@copyright © 2025 Marc Vilagrosa. Todos los derechos reservados.

@brief Inicializa la base de datos SQLite y crea la tabla `Medicion` si no existe.

  DISEÑO:
    (sin entradas) → init_db.py → fichero `mediciones.db` con tabla `Medicion`

  PARÁMETROS:
    Este script no recibe parámetros de entrada (se ejecuta tal cual).
    Ficheros de salida: crea/actualiza `mediciones.db` en el directorio actual.

  NOTAS:
    - Si el fichero `mediciones.db` no existe, SQLite lo crea automáticamente.
    - `CURRENT_TIMESTAMP` almacena fecha/hora en formato UTC por defecto de SQLite.
"""

import sqlite3

# 1) Abrir conexión (si el archivo no existe, se crea en el directorio actual).
conn = sqlite3.connect("mediciones.db")
cursor = conn.cursor()

# 2) Crear la tabla si no existe.
#    Estructura:
#      id: clave primaria autoincremental
#      valor: dato numérico obligatorio (medición)
#      fecha: timestamp de inserción (por defecto, marca temporal de SQLite)
cursor.execute("""
CREATE TABLE IF NOT EXISTS Medicion (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    valor REAL NOT NULL,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
""")

# 3) Confirmar cambios y cerrar conexión.
conn.commit()
conn.close()
print("mediciones.db creado con la tabla Medicion")
