package org.example.btlealumnos2021marc;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/*
 * ============================================================================
 *  @file    ClienteREST.java
 *  @module  org.example.btlealumnos2021marc
 *  @brief   Cliente HTTP mínimo para consumir la API desde Android.
 *           Ejecuta peticiones en segundo plano y entrega (código, cuerpo)
 *           mediante un callback.
 *
 *  @author  Marc Vilagrosa Caturla
 *  @notes   - Si hay fallo de red se devuelve código = -1 y cuerpo = "".
 *           - El Content-Type se fija a application/json; utf-8.
 *
 * ============================================================================
 */
@SuppressWarnings("deprecation") // AsyncTask está deprecado, lo usamos en este Sprint
public class ClienteREST extends AsyncTask<Void, Void, Void> {

    /** Callback simple para entregar el resultado HTTP al finalizar. */
    public interface RespuestaHTTP {
        /**
         * @param codigo Código HTTP (200, 201, 4xx, 5xx). -1 si hubo error de red.
         * @param cuerpo Texto de respuesta (JSON, HTML, vacío, etc.).
         */
        void alFinalizar(int codigo, String cuerpo);
    }

    // -------------------------
    // Parámetros de la petición
    // -------------------------
    private String metodo;     // "GET", "POST", etc.
    private String destino;    // URL completa del endpoint
    private String cuerpo;     // JSON u otro payload (null si no aplica)
    private RespuestaHTTP listener; // callback para devolver el resultado

    // -------------------------
    // Resultado de la petición
    // -------------------------
    private int codigoRespuesta = -1;   // -1 indica error de red/IO
    private String cuerpoRespuesta = ""; // cuerpo textual devuelto por el servidor

    /**
     * Configura y lanza la petición en segundo plano.
     *
     * @param metodo   Verbo HTTP, p. ej. "GET" o "POST".
     * @param destino  URL absoluta hacia la API (http://IP:PUERTO/ruta).
     * @param cuerpo   Payload (normalmente JSON). Usar null para GET.
     * @param listener Implementación del callback para recibir el resultado.
     */
    public void ejecutar(String metodo, String destino, String cuerpo, RespuestaHTTP listener) {
        this.metodo = metodo;
        this.destino = destino;
        this.cuerpo = cuerpo;
        this.listener = listener;
        this.execute(); // dispara AsyncTask: doInBackground() -> onPostExecute()
    }

    /**
     * Trabajo en segundo plano: abre conexión HTTP, envía (si procede) y lee respuesta.
     * No devuelve nada directamente; guardamos en campos y lo entregamos en onPostExecute().
     */
    @Override
    protected Void doInBackground(Void... params) {
        HttpURLConnection conn = null;

        try {
            // 1) Construir conexión
            URL url = new URL(destino);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(metodo);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoInput(true); // queremos leer respuesta

            // 2) Si no es GET y hay cuerpo, escribir el payload en el output stream
            if (!"GET".equalsIgnoreCase(metodo) && cuerpo != null) {
                conn.setDoOutput(true);
                try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                    out.writeBytes(cuerpo);
                    out.flush();
                }
            }

            // 3) Leer código HTTP y el cuerpo (stream de error si 4xx/5xx)
            codigoRespuesta = conn.getResponseCode();
            boolean esError = (codigoRespuesta >= 400);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    esError ? conn.getErrorStream() : conn.getInputStream()
            ))) {
                StringBuilder sb = new StringBuilder();
                String linea;
                while ((linea = br.readLine()) != null) {
                    sb.append(linea);
                }
                cuerpoRespuesta = sb.toString();
            }

        } catch (Exception e) {
            // Cualquier excepción de red/IO la registramos y marcamos código -1
            Log.e("ClienteREST", "Error HTTP: " + e.getMessage());
            codigoRespuesta = -1;
            cuerpoRespuesta = "";
        } finally {
            // 4) Cerrar conexión si existe
            if (conn != null) conn.disconnect();
        }

        // No necesitamos devolver un objeto; usamos los campos de la instancia
        return null;
    }

    /**
     * Hilo principal (UI): entrega el resultado mediante el callback.
     * @param ignored no usado (mantenido por la firma de AsyncTask)
     */
    @Override
    protected void onPostExecute(Void ignored) {
        if (listener != null) {
            listener.alFinalizar(codigoRespuesta, cuerpoRespuesta);
        }
    }
}
