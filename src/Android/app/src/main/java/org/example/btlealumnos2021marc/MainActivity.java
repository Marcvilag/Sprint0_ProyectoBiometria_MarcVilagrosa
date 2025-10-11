package org.example.btlealumnos2021marc;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;

/**
 * MainActivity:
 * Clase principal de la aplicación Android.
 * Se encarga de escanear dispositivos Bluetooth LE (iBeacon),
 * interpretar los datos recibidos y enviar las mediciones al servidor
 * a través de la clase LogicaFake.
 */
public class MainActivity extends AppCompatActivity {

    // -------------------------------------------------------------
    // Constantes y atributos principales
    // -------------------------------------------------------------
    private static final String ETIQUETA_LOG = ">>>>";
    private static final int CODIGO_PETICION_PERMISOS = 11223344;

    private BluetoothLeScanner elEscanner;
    private ScanCallback callbackDelEscaneo = null;

    private LogicaFake logicaFake; // instancia de la lógica que gestiona el envío

    // UUID ASCII que emite tu placa (ajustar según lo detectado con nRF Connect)
    private static final String TARGET_UUID_ASCII = "EPSG-GTI-PROY-3A";

    // Evita enviar múltiples veces la misma medición durante un escaneo
    private volatile boolean medicionEnviada = false;

    // -------------------------------------------------------------
    // Ciclo de vida
    // -------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(ETIQUETA_LOG, "onCreate(): inicia");

        logicaFake = new LogicaFake(); // inicializar lógica de negocio
        inicializarBlueTooth();

        Log.d(ETIQUETA_LOG, "onCreate(): termina");
    }

    // -------------------------------------------------------------
    // Inicialización de Bluetooth y permisos
    // -------------------------------------------------------------
    private void inicializarBlueTooth() {
        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): preparando adaptador");

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) {
            toast("Este dispositivo no soporta Bluetooth");
            return;
        }

        // Solicitud de permisos según versión de Android
        if (!tienePermisosNecesarios()) {
            pedirPermisosNecesarios();
            return;
        }

        // Encender Bluetooth si está apagado
        // Encender Bluetooth si está apagado
        if (!bta.isEnabled()) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(enableBtIntent);
                } else {
                    pedirPermisosNecesarios();
                }
            } catch (SecurityException e) {
                Log.e(ETIQUETA_LOG, "Sin permiso BLUETOOTH_CONNECT para encender Bluetooth: " + e.getMessage());
            }
        }


        elEscanner = bta.getBluetoothLeScanner();
        if (elEscanner == null) {
            Log.e(ETIQUETA_LOG, "No se pudo obtener el escáner BTLE");
        }
    }

    // -------------------------------------------------------------
    // Botones de la interfaz
    // -------------------------------------------------------------
    public void botonBuscarDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, "botón buscar todos los dispositivos pulsado");
        buscarTodosLosDispositivosBTLE();
    }

    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, "botón buscar nuestro dispositivo pulsado");
        buscarEsteDispositivoBTLE("Grupo4"); // cambia el nombre si tu placa usa otro
    }

    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, "botón detener búsqueda pulsado");
        detenerBusquedaDispositivosBTLE();
    }

    // -------------------------------------------------------------
    // Escaneo BLE general (todos los dispositivos)
    // -------------------------------------------------------------
    private void buscarTodosLosDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): empieza");

        if (!tienePermisosNecesarios()) {
            pedirPermisosNecesarios();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !ubicacionActiva()) {
            toast("Activa la ubicación del dispositivo para escanear BLE");
            try {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } catch (Exception ignored) {}
            return;
        }

        medicionEnviada = false;

        callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                if (resultado.getScanRecord() == null) return;

                mostrarInformacionDispositivoBTLE(resultado);

                byte[] bytes = resultado.getScanRecord().getBytes();
                TramaIBeacon tib = new TramaIBeacon(bytes);

                String uuidAscii = Utilidades.bytesToString(tib.getUUID());
                if (TARGET_UUID_ASCII.equals(uuidAscii)) {
                    int valor = Utilidades.bytesToInt(tib.getMinor());

                    if (!medicionEnviada) {
                        medicionEnviada = true;
                        logicaFake.guardarMedicion("Temperatura", valor);
                        detenerBusquedaDispositivosBTLE();
                    }
                }
            }
        };

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                elEscanner.startScan(null, settings, callbackDelEscaneo);
                toast("Escaneo BLE iniciado");
            } else {
                pedirPermisosNecesarios();
            }
        } catch (SecurityException e) {
            Log.e(ETIQUETA_LOG, "Sin permiso BLUETOOTH_SCAN: " + e.getMessage());
        }


    }

    // -------------------------------------------------------------
    // Escaneo BLE con filtro por nombre
    // -------------------------------------------------------------
    private void buscarEsteDispositivoBTLE(final String dispositivoBuscado) {
        Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): buscando " + dispositivoBuscado);

        if (!tienePermisosNecesarios()) {
            pedirPermisosNecesarios();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !ubicacionActiva()) {
            toast("Activa la ubicación del dispositivo para escanear BLE");
            try {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } catch (Exception ignored) {}
            return;
        }

        medicionEnviada = false;

        callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                mostrarInformacionDispositivoBTLE(resultado);
            }
        };

        ScanFilter filtro = new ScanFilter.Builder()
                .setDeviceName(dispositivoBuscado)
                .build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                elEscanner.startScan(Arrays.asList(filtro), settings, callbackDelEscaneo);
                toast("Escaneo BLE (filtro por nombre) iniciado");
            } else {
                pedirPermisosNecesarios();
            }
        } catch (SecurityException e) {
            Log.e(ETIQUETA_LOG, "Sin permiso BLUETOOTH_SCAN: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Detener búsqueda
    // -------------------------------------------------------------
    private void detenerBusquedaDispositivosBTLE() {
        if (callbackDelEscaneo == null) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                elEscanner.stopScan(callbackDelEscaneo);
                toast("Escaneo BLE detenido");
            } catch (SecurityException e) {
                Log.e(ETIQUETA_LOG, "Sin permiso BLUETOOTH_SCAN para detener escaneo");
            }
        }
        callbackDelEscaneo = null;
    }

    // -------------------------------------------------------------
    // Mostrar información del dispositivo detectado
    // -------------------------------------------------------------
    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {
        BluetoothDevice dispositivo = resultado.getDevice();
        byte[] bytes = (resultado.getScanRecord() != null) ? resultado.getScanRecord().getBytes() : null;
        int rssi = resultado.getRssi();

        Log.d(ETIQUETA_LOG, "****** DISPOSITIVO DETECTADO ******");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, "nombre = " + dispositivo.getName());
            Log.d(ETIQUETA_LOG, "dirección = " + dispositivo.getAddress());
        }
        if (bytes == null) return;

        TramaIBeacon tib = new TramaIBeacon(bytes);
        Log.d(ETIQUETA_LOG, "uuid(ascii) = " + Utilidades.bytesToString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, "major(int) = " + Utilidades.bytesToInt(tib.getMajor()));
        Log.d(ETIQUETA_LOG, "minor(int) = " + Utilidades.bytesToInt(tib.getMinor()));
        Log.d(ETIQUETA_LOG, "rssi = " + rssi + " txPower = " + tib.getTxPower());
    }

    // -------------------------------------------------------------
    // Permisos y utilidades auxiliares
    // -------------------------------------------------------------
    private boolean tienePermisosNecesarios() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void pedirPermisosNecesarios() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                    CODIGO_PETICION_PERMISOS
            );
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    CODIGO_PETICION_PERMISOS
            );
        }
    }

    private boolean ubicacionActiva() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean gps = lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean net = lm != null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            return gps || net;
        } catch (Exception e) {
            return false;
        }
    }

    private void toast(String s) {
        runOnUiThread(() -> Toast.makeText(this, s, Toast.LENGTH_SHORT).show());
    }

    // -------------------------------------------------------------
    // Resultado de solicitud de permisos
    // -------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODIGO_PETICION_PERMISOS) {
            boolean ok = grantResults.length > 0;
            if (ok) {
                for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) ok = false;
            }
            Log.d(ETIQUETA_LOG, "Permisos concedidos = " + ok);
            if (ok) inicializarBlueTooth();
        }
    }
}
