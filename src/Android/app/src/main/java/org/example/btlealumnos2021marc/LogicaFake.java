package org.example.btlealumnos2021marc;

import android.util.Log;

/**
 * LogicaFake:
 * Clase que simula la lógica del cliente Android.
 * Se encarga de enviar las mediciones al servidor mediante ClienteREST.
 */
public class LogicaFake {

    private static final String ETIQUETA = "LogicaFake";
    private static final String URL_SERVIDOR = "http://10.0.2.2:8000/api/v1/mediciones";

    /**
     * Envía una medición JSON al servidor (modo real o simulado).
     * @param tipo  Tipo de sensor (ej. "Temperatura")
     * @param valor Valor medido
     */
    public void guardarMedicion(String tipo, double valor) {
        String json = "{\"tipo\":\"" + tipo + "\",\"valor\":" + valor + "}";
        Log.d(ETIQUETA, "Enviando medición: " + json);

        ClienteREST cliente = new ClienteREST();
        cliente.ejecutar("POST", URL_SERVIDOR, json, (codigo, cuerpo) -> {
            if (codigo >= 200 && codigo < 300) {
                Log.d(ETIQUETA, "✅ Envío correcto (" + codigo + "): " + cuerpo);
            } else {
                Log.e(ETIQUETA, "❌ Error (" + codigo + "): " + cuerpo);
            }
        });
    }
}
