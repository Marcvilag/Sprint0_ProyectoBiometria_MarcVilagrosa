# Sprint 0 — Proyecto **Biometría**

## Descripción del proyecto
Es un sistema que integra varias tecnologías.  
Un Arduino actúa como emisor BLE (beacon), enviando información de forma periódica. Una aplicación Android recibe esas tramas BLE, las procesa y las reenvía mediante HTTP hacia una API REST La API, desarrollada con FastAPI, se encarga de almacenar los datos en una base de datos SQLite.  
Además, se incluye una interfaz web sencilla (HTML + JavaScript) que consulta la API y permite visualizar las mediciones registradas.  

De esta forma, el sistema combina captura de datos (Arduino), recepción y reenvío (Android), almacenamiento y exposición (API + BD) y visualización (Web), todo dentro de una arquitectura clara y modular.  

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

## Estado actual
La estructura básica del repositorio ya está creada y se ha solucionado .  
Las instrucciones para **desplegar el proyecto** y la forma de **ejecutar los tests** aún no están disponibles, pero se añadirán en próximas fases.  

