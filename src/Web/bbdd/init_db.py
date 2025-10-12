# -*- coding: utf-8 -*-
"""
@file init_db.py
@author Marc Vilagrosa
@copyright © 2025 Marc Vilagrosa. Todos los derechos reservados.

@brief Inicializa la base de datos SQLite y crea la tabla `Medicion` si no existe.

@details
  DESCRIPCIÓN:
    Script autónomo para preparar la base de datos de la aplicación.
    Conecta a un fichero SQLite llamado `mediciones.db` y asegura la
    existencia de la tabla `Medicion` con las columnas:
      - id (INTEGER, PK autoincremental)
      - valor (REAL, obligatorio)
      - fecha (TIMESTAMP, por defecto CURRENT_TIMESTAMP)

  DISEÑO:
    ∅ (sin entradas) → init_db.py → fichero `mediciones.db` con tabla `Medicion`
    Pasos:
      1) Conectar/crear `mediciones.db`.
      2) Crear tabla `Medicion` con CREATE TABLE IF NOT EXISTS.
      3) Confirmar cambios (commit) y cerrar la conexión.
      4) Informar por consola del resultado.

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
