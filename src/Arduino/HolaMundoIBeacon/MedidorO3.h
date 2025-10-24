// -- mode: c++ --
// MedidorO3.h  (ULPSM O3 → ppm - Solución de errores para HolaMundoIBeacon.ino)

#ifndef MEDIDOR_O3_H_INCLUIDO
#define MEDIDOR_O3_H_INCLUIDO

#include <Arduino.h>
#include <math.h> // Necesario para usar fabsf()

// ===================== TUS CONSTANTES DE CONFIGURACIÓN (O3_...) =====================
// Estas son las MACROS que tu HolaMundoIBeacon.ino espera.
#ifndef O3_PIN_VGAS
#define O3_PIN_VGAS A5 
#endif
#ifndef O3_PIN_VREF
#define O3_PIN_VREF A4 
#endif

#ifndef O3_VDD
#define O3_VDD 3.30f        // Voltaje de alimentación del sensor/ADC
#endif

#ifndef O3_ADC_BITS
#define O3_ADC_BITS 12      // Resolución del ADC (12 bits)
#endif

// Constantes de calibración (dentro del .h para ser accesibles por la lógica de medición)
const float SENSIBILIDAD_SENSOR = -44.26f;  // Tu sensibilidad original (nA/ppm)
const float GAIN_TIA = 499.0f;              // Ganancia del amplificador (Ω)

// ===================== Clase de medida O3 =====================
class MedidorO3 { // <--- Nombre de clase CORREGIDO
public:
    // El constructor original (ajustado para la versión simplificada)
    explicit MedidorO3() 
    : dV0(0) {} 

    // Función iniciar() (Llamada en setup, ahora comentada internamente)
    void iniciar(int muestrasZero = 100) { 
        /* hacerZero(muestrasZero); // Se mantiene comentado según tu solicitud */ 
    }

    // Función leerVref() (Llamada en setup, implementada usando leerVolt)
    float leerVref(int nAvg = 20) { return leerVolt(O3_PIN_VREF, nAvg); }

    // Función hacerZero() (Mantendremos esta estructura si la quieres descomentar luego)
    void hacerZero(int N = 100) {
        /*
        // [Lógica original de ZERO, mantenida comentada]
        float sG=0, sR=0;
        for (int i=0; i<N; ++i) { 
            sG += leerVolt(O3_PIN_VGAS, 1);
            sR += leerVolt(O3_PIN_VREF, 1);
            delay(10);
        }
        dV0 = (sG/N) - (sR/N); 
        */
    }

    // Función medirPPM() (Llamada en loop, implementa la lógica simplificada)
    float medirPPM() {
        float Vg = leerVolt(O3_PIN_VGAS, 10);
        float Vr = leerVolt(O3_PIN_VREF, 10);
        
        // Diferencia de voltaje BRUTA (V)
        float deltaV = Vg - Vr; 

        // Cálculo del denominador (GAIN_TIA * S * 1e-6)
        float denominador = GAIN_TIA * SENSIBILIDAD_SENSOR * 1e-6f; 
        
        float ppm = 0.0f;
        if (denominador != 0.0f) {
            // Conversión sin aplicar corrección de ZERO
            ppm = deltaV / denominador;
        }

        // Aplicar valor absoluto (fabsf) según tu solicitud
        ppm = fabsf(ppm); 
        
        return ppm;
    }
    
    // (Omitimos setSensitivityNAppm, getSensitivityNAppm, getM_V_per_ppm, ya no se usan)
    float getZero_dV() const { return dV0; } 

private:
    float leerVolt(int pin, int nAvg = 10) {
        long acc = 0;
        for (int i=0; i<nAvg; ++i) { acc += analogRead(pin); delay(2); }
        const float raw = (float)acc / nAvg;
        const float fullScale = (float)((1 << O3_ADC_BITS) - 1); 
        return (raw * O3_VDD) / fullScale;
    }

    float dV0; 
};

#endif // MEDIDOR_O3_H_INCLUIDO