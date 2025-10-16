package org.example.btlealumnos2021marc;

import android.util.Log;

/*
 * ============================================================================
 *  @file    LogicaFake.java
 *  @module  org.example.btlealumnos2021marc
 *  @brief   Capa mínima de “lógica de negocio” en el cliente Android.
 *           Publica una medición al servidor FastAPI mediante POST JSON.
 *
 *  @details Envía solo el campo { "valor": <número> }.
 *           La API se encarga de:
 *             - generar id autoincrement,
 *             - sellar fecha/hora en la BD.
 *
 *           Redes:
 *           - Dispositivo físico → IP LAN del PC (misma red/hotspot)
 *             Ej.: hotspot iPhone suele ser 172.20.10.X
 *
 *  @author  Marc Vilagrosa Caturla
 *  @notes   - Requiere permiso INTERNET en AndroidManifest.
 *           - Si usas HTTP sin TLS, añade android:usesCleartextTraffic="true".
 *           - Este cliente delega el HTTP real en ClienteREST (AsyncTask).
 * ============================================================================
 */
public class LogicaFake {

    /** Tag para logs (Logcat). */
    private static final String ETIQUETA = "LogicaFake";

    // ------------------------------------------------------------------------
    // Destino de la API (elige el que toque según tu entorno)
    // ------------------------------------------------------------------------

    // Emulador Android (AVD) → host PC:
    // private static final String URL_SERVIDOR = "http://10.0.2.2:8000/api/v1/mediciones";

    // Dispositivo físico → PC por IP LAN (MISMA RED / HOTSPOT):
    // Cambia la IP a la de tu PC en cada red (ipconfig -> “Dirección IPv4”).
    private static final String URL_SERVIDOR = "http://172.20.10.11:8000/api/v1/mediciones"; // <-- tu IP

    // ------------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------------

    /**
     * Guarda una medición a partir de un entero (típico: valor leído del MINOR).
     * Convierte a double y delega en {@link #guardarMedicion(double)}.
     *
     * @param valor valor entero (p.ej., 235 o -12).
     */
    public void guardarMedicion(int valor) {
        guardarMedicion((double) valor);
    }

    /**
     * Envía la medición al servidor como JSON vía POST.
     * Cuerpo: {@code {"valor": <numero>}}
     *
     * @param valor valor numérico a persistir (double para abarcar enteros y decimales).
     */
    public void guardarMedicion(double valor) {
        // Construcción del JSON mínimo esperado por la API
        String json = "{\"valor\":" + valor + "}";
        Log.d(ETIQUETA, "Enviando medición: " + json);

        // Dispara la petición HTTP asíncrona
        ClienteREST cliente = new ClienteREST();
        cliente.ejecutar("POST", URL_SERVIDOR, json, (codigo, cuerpo) -> {
            // Callback al finalizar: loguea éxito o error con el cuerpo devuelto por FastAPI
            if (codigo >= 200 && codigo < 300) {
                Log.d(ETIQUETA, "OK (" + codigo + "): " + cuerpo);
            } else {
                Log.e(ETIQUETA, "Error (" + codigo + "): " + cuerpo);
            }
        });
    }
}
