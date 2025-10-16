package org.example.btlealumnos2021marc;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * ClienteREST (simple):
 * Ejecuta peticiones HTTP en segundo plano y entrega (codigo, cuerpo) en un callback.
 * Nota: AsyncTask está deprecado, pero sirve para el Sprint 0.
 */
public class ClienteREST extends AsyncTask<Void, Void, Void> {

    /** Callback para entregar el resultado HTTP al finalizar. */
    public interface RespuestaHTTP {
        void alFinalizar(int codigo, String cuerpo);
    }

    private String metodo;
    private String destino;
    private String cuerpo;
    private RespuestaHTTP listener;

    private int codigoRespuesta = -1;  // -1 → error de red
    private String cuerpoRespuesta = "";

    /**
     * Configura y lanza la petición.
     * @param metodo   "GET", "POST", etc.
     * @param destino  URL completa del endpoint
     * @param cuerpo   JSON u otro payload (null si no aplica)
     * @param listener callback para recibir (codigo, cuerpo)
     */
    public void ejecutar(String metodo, String destino, String cuerpo, RespuestaHTTP listener) {
        this.metodo = metodo;
        this.destino = destino;
        this.cuerpo = cuerpo;
        this.listener = listener;
        this.execute();
    }

    @Override
    protected Void doInBackground(Void... params) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(destino);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(metodo);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoInput(true);

            // Si no es GET y hay cuerpo, lo enviamos
            if (!"GET".equalsIgnoreCase(metodo) && cuerpo != null) {
                conn.setDoOutput(true);
                try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                    out.writeBytes(cuerpo);
                    out.flush();
                }
            }

            // Código HTTP y lectura de respuesta (ok o error)
            codigoRespuesta = conn.getResponseCode();
            boolean esError = (codigoRespuesta >= 400);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    esError ? conn.getErrorStream() : conn.getInputStream()
            ))) {
                StringBuilder sb = new StringBuilder();
                String linea;
                while ((linea = br.readLine()) != null) sb.append(linea);
                cuerpoRespuesta = sb.toString();
            }
        } catch (Exception e) {
            Log.e("ClienteREST", "Error HTTP: " + e.getMessage());
            codigoRespuesta = -1;
            cuerpoRespuesta = "";
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null; // no necesitamos resultado; pasamos todo por el callback
    }

    @Override
    protected void onPostExecute(Void ignored) {
        if (listener != null) {
            listener.alFinalizar(codigoRespuesta, cuerpoRespuesta);
        }
    }
}
