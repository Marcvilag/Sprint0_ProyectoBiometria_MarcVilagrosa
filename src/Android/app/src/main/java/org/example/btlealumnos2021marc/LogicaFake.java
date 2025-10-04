package org.example.btlealumnos2021marc;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase que simula las solicitudes al backend del proyecto.
 * No realiza comunicación real; devuelve datos de ejemplo
 * para pruebas antes de tener la API REST implementada.
 */
public class LogicaFake {

    /**
     * Simula la obtención de la última medición registrada.
     * @return Mapa con los datos de una medición de ejemplo.
     */
    public Map<String, Object> solicitarUltimaMedicion() {
        Map<String, Object> medicion = new HashMap<>();
        medicion.put("id", 999);
        medicion.put("valor", 22.53);
        medicion.put("fecha", "2025-10-02 12:29:40");
        return medicion;
    }

    /**
     * Simula el envío de una nueva medición al backend.
     * @param valor Valor de la medición a enviar.
     * @return true si la simulación es exitosa.
     */
    public boolean solicitarEnvioMedicion(double valor) {
        System.out.println("Simulando envío de medición (fake): " + valor);
        return true; // siempre simula éxito
    }
}
