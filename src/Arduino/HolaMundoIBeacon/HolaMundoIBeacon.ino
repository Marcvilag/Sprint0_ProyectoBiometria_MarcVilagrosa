// -*-c++-*-
//
// --------------------------------------------------------------
//
// Jordi Bataller i Mascarell
// 2019-07-07
//
// --------------------------------------------------------------

// https://learn.sparkfun.com/tutorials/nrf52840-development-with-arduino-and-circuitpython
// https://stackoverflow.com/questions/29246805/can-an-ibeacon-have-a-data-payload

// --------------------------------------------------------------
// --------------------------------------------------------------
#include <bluefruit.h>

#undef min 
#undef max 

// --------------------------------------------------------------
// --------------------------------------------------------------
#include "LED.h"
#include "PuertoSerie.h"

// --------------------------------------------------------------
// --------------------------------------------------------------
namespace Globales {

  LED elLED ( /* NUMERO DEL PIN LED = */ 7 );

  PuertoSerie elPuerto ( /* velocidad = */ 115200 ); // 115200 o 9600 o ...

  // Serial1 en el ejemplo de Curro creo que es la conexión placa-sensor
};

// --------------------------------------------------------------
// --------------------------------------------------------------
#include "EmisoraBLE.h"
#include "Publicador.h"
#include "Medidor.h"

// ===================== O3 · AÑADIDO (BEGIN) =====================
#include "MedidorO3.h"   // medidor de O3 (ULPSM)

// UUID para iBeacon (puedes cambiarlo por el vuestro)
static uint8_t O3_BEACON_UUID[16] = {
  0x12,0x34,0x56,0x78, 0x90,0xAB,0xCD,0xEF,
  0x12,0x34,0x56,0x78, 0x90,0xAB,0xCD,0xEF
};
// ===================== O3 · AÑADIDO (END) =======================


// --------------------------------------------------------------
// --------------------------------------------------------------
namespace Globales {

  Publicador elPublicador;

  Medidor elMedidor;

  // ===================== O3 · AÑADIDO (BEGIN) =====================
  MedidorO3 elMedidorO3;   // instancia para medir O3
  // ===================== O3 · AÑADIDO (END) =======================

}; // namespace

// --------------------------------------------------------------
// --------------------------------------------------------------
void inicializarPlaquita () {

  // de momento nada

} // ()

// --------------------------------------------------------------
// setup()
// --------------------------------------------------------------
void setup() {

  Globales::elPuerto.esperarDisponible();

  // *** CAMBIO 1: usar ADC a 12 bits (no es oversampling) ***
  analogReadResolution(12);   // LSB ≈ 0.805 mV con VDD=3.3V

  inicializarPlaquita();

  // Globales::elPublicador.laEmisora.pruebaEmision();
  Globales::elPublicador.encenderEmisora();

  Globales::elMedidor.iniciarMedidor();

  // ===================== O3 · AÑADIDO (BEGIN) =====================
  // Si ya conoces S del QR, descomenta y pon tu valor de S (nA/ppm):
  // Globales::elMedidorO3.setSensitivityNAppm( /* S_del_QR_en_nA_por_ppm */ );

  // Comprobación rápida de Vref (~1.65 V)
  {
    float vref = Globales::elMedidorO3.leerVref(20); // <--- CORRECCIÓN 1: leerVref OK
    Globales::elPuerto.escribir("O3 Vref mV ≈ ");
    Globales::elPuerto.escribir( (int)(vref*1000.0f) );
    Globales::elPuerto.escribir("\n");
  }

  // *** CAMBIO 2: pequeño warm-up antes del ZERO ***
  esperar(3000);                      // 3 s (si puedes 30–60 s, mejor para practicas)

  // ZERO inicial en aire limpio (~1 s en tu función)
  Globales::elMedidorO3.iniciar(100);
  // ===================== O3 · AÑADIDO (END) =======================

  Globales::elPuerto.escribir( "---- setup(): fin ---- \n " );
} // setup ()

// --------------------------------------------------------------
// --------------------------------------------------------------
inline void lucecitas() {
  using namespace Globales;

  elLED.brillar( 100 ); // 100 encendido
  esperar ( 400 ); //  100 apagado
  elLED.brillar( 100 ); // 100 encendido
  esperar ( 400 ); //  100 apagado
  Globales::elLED.brillar( 100 ); // 100 encendido
  esperar ( 400 ); //  100 apagado
  Globales::elLED.brillar( 1000 ); // 1000 encendido
  esperar ( 1000 ); //  100 apagado
} // ()

// --------------------------------------------------------------
// loop ()
// --------------------------------------------------------------
namespace Loop {
  uint8_t cont = 0;
};

// ..............................................................
// ..............................................................
void loop () {

  using namespace Loop;
  using namespace Globales;

  cont++;

  elPuerto.escribir( "\n---- loop(): empieza " );
  elPuerto.escribir( cont );
  elPuerto.escribir( "\n" );

  lucecitas();

  // ===================== O3 · MEDIR + PUBLICAR (BEGIN) =================
  
  // 1) Medimos O3 en PPM (float).
  float o3_ppm = elMedidorO3.medirPPM(); // Llama a la función PPM (corregida)

  // ** NOTA IMPORTANTE: Para la emisión iBeacon, el MINOR DEBE SER UN ENTERO (uint16_t).
  //  Convertimos PPM a PPB (multiplicando por 1000) para enviar un valor entero más útil.
  //  Ej: 0.1 ppm se convierte en 100 ppb.
  uint16_t o3_ppb_para_beacon = (uint16_t)lroundf(o3_ppm * 1000.0f);


  // 2) Emitimos iBeacon con NOMBRE "Grupo4"
  elPublicador.laEmisora.emitirAnuncioIBeaconConNombre(
    "Grupo4",             // nombre para filtrar por NOMBRE en Android
    O3_BEACON_UUID,
    (int16_t)cont,        // major = contador de bucle
    (int16_t)o3_ppb_para_beacon // minor = O3 en ppb (entero)
  );

  // 3) Log de depuración: MUESTRA EL VALOR EN PPM (float)
  elPuerto.escribir( "O3 → ppm=" );
  // Usamos elPuerto.escribir para float si tu clase lo soporta. Si no, usa Serial.print.
  // Asumiendo que PuertoSerie soporta float para fines de depuración:
  // <--- CORRECCIÓN 2: Usar Serial.print para el float --->
  Serial.print(o3_ppm, 4); 
  Serial.print(" ppm"); 
  elPuerto.escribir( "\n" );
  // ===================== O3 · MEDIR + PUBLICAR (END) ===================
  
  // ... resto del código ...

  elPuerto.escribir( "---- loop(): acaba **** " );
  elPuerto.escribir( cont );
  elPuerto.escribir( "\n" );


  // --- DEBUG: leer crudo y en voltios con 10 y 12 bits ---
  // Se deja la salida Serial.print original para depuración detallada
  int rawRef = analogRead(O3_PIN_VREF);
  int rawGas = analogRead(O3_PIN_VGAS);

  float vref12 = rawRef * O3_VDD / ((1 << O3_ADC_BITS) - 1);
  float vgas12 = rawGas * O3_VDD / ((1 << O3_ADC_BITS) - 1);
  float vref10 = rawRef * O3_VDD / 1023.0;
  float vgas10 = rawGas * O3_VDD / 1023.0;

  Serial.print("RAW A1="); Serial.print(rawRef);
  Serial.print("  RAW A3="); Serial.print(rawGas);
  Serial.print("  Vref12="); Serial.print(vref12*1000,0); Serial.print("mV");
  Serial.print("  Vgas12="); Serial.print(vgas12*1000,0); Serial.print("mV");
  Serial.print("  Vref10="); Serial.print(vref10*1000,0); Serial.print("mV");
  Serial.print("  Vgas10="); Serial.println(vgas10*1000,0);

} // loop ()
// --------------------------------------------------------------