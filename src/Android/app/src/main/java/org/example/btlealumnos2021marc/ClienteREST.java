package org.example.btlealumnos2021marc;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * ClienteREST:
 * Clase que realiza peticiones HTTP (GET o POST) en segundo plano,
 * notificando el resultado a través de una interfaz de callback.
 */
public class ClienteREST extends AsyncTask<Void, Void, Boolean> {

    /**
     * Interfaz para recibir la respuesta de la petición HTTP.
     */
    public interface RespuestaHTTP {
        void alFinalizar(int codigo, String cuerpo);
    }

    private String metodo;
    private String destino;
    private String cuerpo;
    private RespuestaHTTP listener;
    private int codigoRespuesta;
    private String cuerpoRespuesta = "";

    /**
     * Configura y lanza una petición HTTP asíncrona.
     * @param metodo  Tipo de método HTTP (p. ej. "GET" o "POST")
     * @param destino URL del servidor
     * @param cuerpo  Cuerpo de la petición (puede ser null)
     * @param listener Callback para procesar la respuesta
     */
    public void ejecutar(String metodo, String destino, String cuerpo, RespuestaHTTP listener) {
        this.metodo = metodo;
        this.destino = destino;
        this.cuerpo = cuerpo;
        this.listener = listener;
        this.execute();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            URL url = new URL(destino);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(metodo);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoInput(true);

            if (!"GET".equals(metodo) && cuerpo != null) {
                conn.setDoOutput(true);
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.writeBytes(cuerpo);
                out.flush();
                out.close();
            }

            codigoRespuesta = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (codigoRespuesta >= 400) ? conn.getErrorStream() : conn.getInputStream()
            ));

            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) sb.append(linea);
            cuerpoRespuesta = sb.toString();
            conn.disconnect();

            return true;
        } catch (Exception e) {
            Log.e("ClienteREST", "Error HTTP: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean ok) {
        if (listener != null) {
            listener.alFinalizar(codigoRespuesta, cuerpoRespuesta);
        }
    }
}
