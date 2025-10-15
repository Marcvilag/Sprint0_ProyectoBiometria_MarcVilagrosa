package org.example.btlealumnos2021marc;

import android.util.Log;

/**
 * LogicaFake:
 * Envía la medición al servidor (solo 'valor').
 * La API asigna id autoincrement y timestamp.
 */
public class LogicaFake {

    private static final String ETIQUETA = "LogicaFake";
    // Emulador Android → host PC: 10.0.2.2
    //private static final String URL_SERVIDOR = "http://10.0.2.2:8000/api/v1/mediciones";

    // Móvil físico → PC por IP LAN:
    private static final String URL_SERVIDOR = "http://192.168.18.101:8000/api/v1/mediciones"; // <-- pon tu IP

    /** Acepta int (desde el minor) y delega a double */
    public void guardarMedicion(int valor) {
        guardarMedicion((double) valor);
    }

    /** Envía JSON { "valor": <numero> } por POST */
    public void guardarMedicion(double valor) {
        String json = "{\"valor\":" + valor + "}";
        Log.d(ETIQUETA, "Enviando medición: " + json);

        ClienteREST cliente = new ClienteREST();
        cliente.ejecutar("POST", URL_SERVIDOR, json, (codigo, cuerpo) -> {
            if (codigo >= 200 && codigo < 300) {
                Log.d(ETIQUETA, "OK (" + codigo + "): " + cuerpo);
            } else {
                Log.e(ETIQUETA, "Error (" + codigo + "): " + cuerpo);
            }
        });
    }
}
