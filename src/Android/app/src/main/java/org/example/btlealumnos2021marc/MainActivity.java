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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Flujo:
 * - "Conectar (detectar \"Grupo4\")": inicia escaneo continuo (no se detiene solo).
 *   • Muestra un Toast de conectado solo una vez.
 *   • Va cacheando el último MINOR=ppb recibido.
 * - "Medir O₃": usa el valor cacheado (ppb → ppm) y lo pinta. No toca el escaneo.
 * - "Detener búsqueda": para el escaneo.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BEACON";
    private static final int REQ_PERMISOS = 11223344;
    private static final int REQ_BT_ON    = 778899;

    private static final String NOMBRE_BEACON = "Grupo4";

    private BluetoothLeScanner escaner;
    private ScanCallback callback;
    private LogicaFake logicaFake; // (no se usa en el flujo nuevo)

    // Estado
    private volatile boolean conectado = false;    // conectado lógico (detectado al menos una vez)
    private volatile boolean escaneando = false;   // para saber si hay un scan activo
    private Integer ultimoMinorPpb = null;         // última lectura (ppb) 0..65535
    private TextView txtO3;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logicaFake = new LogicaFake();
        txtO3 = findViewById(R.id.textO3);
        inicializarBluetooth();
    }

    // ---------- Botones ----------
    public void botonBuscarDispositivosBTLEPulsado(View v) { iniciarEscaneoGeneral(); }
    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) { detener(); }
    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) { iniciarEscaneoConectar(); } // CONECTAR por nombre (continuo)
    public void botonMedirO3Pulsado(View v) { mostrarLecturaO3(); } // lee cache y pinta

    // ---------- Inicialización ----------
    private void inicializarBluetooth() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) { log("Este dispositivo no soporta Bluetooth"); toast("Sin Bluetooth"); return; }

        if (!permisosOk()) { pedirPermisos(); return; }

        if (!bta.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_BT_ON);
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            escaner = bta.getBluetoothLeScanner();
        }
        if (escaner == null) log("No se pudo obtener el escáner LE (aún)");
    }

    // ---------- CONECTAR: detectar por nombre y mantener escaneo activo ----------
    private void iniciarEscaneoConectar() {
        log("Conectar: buscando \"" + NOMBRE_BEACON + "\"");
        if (!precondicionesOk()) return;

        if (escaneando) {
            toast("Escaneo ya activo");
            return;
        }

        List<ScanFilter> filtros = new ArrayList<>();
        filtros.add(new ScanFilter.Builder().setDeviceName(NOMBRE_BEACON).build());

        conectado = false;         // reset aviso
        ultimoMinorPpb = null;     // limpia última lectura

        callback = new ScanCallback() {
            @Override public void onScanResult(int callbackType, ScanResult r) {
                if (r == null || r.getScanRecord() == null) return;

                String nombre = obtenerNombreSeguro(r.getDevice());
                if (nombre == null || !NOMBRE_BEACON.equals(nombre)) return;

                // 1) Aviso “conectado” solo la primera vez
                if (!conectado) {
                    conectado = true;
                    toast("Conectado (detectado): " + NOMBRE_BEACON);
                    log("Conectado lógico a " + NOMBRE_BEACON + " (escaneo sigue activo)");
                }

                // 2) Cachear última lectura O3 (ppb) desde iBeacon (minor, sin signo)
                byte[] adv = r.getScanRecord().getBytes();
                if (adv == null) return;

                int[] mm = extractMajorMinorFromIBeacon(adv);
                if (mm[1] >= 0) {
                    int minorPpb = mm[1] & 0xFFFF;   // *** CLAVE: interpretar como unsigned 16-bit
                    ultimoMinorPpb = minorPpb;       // cache 0..65535
                }
            }
            @Override public void onScanFailed(int errorCode) {
                Log.e(TAG, "Scan failed: " + errorCode);
            }
        };

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            escaner.startScan(filtros, settings, callback);
            escaneando = true;  // seguimos escaneando hasta pulsar "Detener"
            toast("Escaneando \"" + NOMBRE_BEACON + "\"… (se detiene con el botón)");
        } else {
            pedirPermisos();
        }
    }

    // ---------- MEDIR O3: lee el último valor cacheado (no toca el escaneo) ----------
    private void mostrarLecturaO3() {
        if (ultimoMinorPpb == null) {
            toast("Aún sin dato de O₃. Pulsa Conectar o espera un momento…");
            return;
        }
        double ppm = ultimoMinorPpb / 1000.0;
        if (txtO3 != null) {
            txtO3.setText(String.format(java.util.Locale.US, "O₃: %.3f ppm", ppm));
        }
        log("O3 → minor(ppb)=" + ultimoMinorPpb + "  ppm=" + ppm);
        toast(String.format(java.util.Locale.US, "Medición O₃: %.3f ppm", ppm));
    }

    // ---------- Escaneo general (depuración opcional) ----------
    private void iniciarEscaneoGeneral() {
        log("Escaneo general: empieza");
        if (!precondicionesOk()) return;

        if (escaneando) {
            toast("Ya hay un escaneo activo (Detén antes si quieres el general).");
            return;
        }

        callback = new ScanCallback() {
            @Override public void onScanResult(int callbackType, ScanResult r) {
                if (r == null || r.getScanRecord() == null) return;
                byte[] adv = r.getScanRecord().getBytes();
                if (adv == null) return;

                int[] mm = extractMajorMinorFromIBeacon(adv);
                if (mm[1] >= 0) {
                    int minor = mm[1] & 0xFFFF; // *** también sin signo en logs
                    Log.d(TAG, "iBeacon → major=" + mm[0] + " minor=" + minor);
                }
            }
        };

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            escaner.startScan(null, settings, callback);
            escaneando = true;
            toast("Escaneo BLE iniciado (general)");
        } else {
            pedirPermisos();
        }
    }

    private void detener() {
        if (callback == null || escaner == null) { toast("Escaneo detenido"); escaneando = false; return; }
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                escaner.stopScan(callback);
            }
        } catch (Exception ignored) {}
        callback = null;
        escaneando = false;
        toast("Escaneo detenido");
    }

    // ---------- Parseo iBeacon: devuelve {major, minor} o {-1,-1} ----------
    private static int[] extractMajorMinorFromIBeacon(byte[] adv) {
        if (adv == null || adv.length < 30) return new int[]{-1,-1};

        int i = 0;
        while (i < adv.length) {
            int len = adv[i] & 0xFF;
            if (len == 0) break;
            int typeIdx = i + 1;
            int dataIdx = i + 2;
            if (typeIdx >= adv.length) break;
            int type = adv[typeIdx] & 0xFF;

            if (type == 0xFF) { // Manufacturer Data
                int structEnd = i + 1 + len;
                if (dataIdx + 4 <= structEnd) {
                    boolean isAppleIBeacon =
                            (adv[dataIdx]     == (byte)0x4C) &&
                                    (adv[dataIdx + 1] == (byte)0x00) &&
                                    (adv[dataIdx + 2] == (byte)0x02) &&
                                    (adv[dataIdx + 3] == (byte)0x15);
                    if (isAppleIBeacon) {
                        int majorIdx = dataIdx + 4 + 16;  // 4 hdr + 16 UUID
                        int minorIdx = majorIdx + 2;
                        if (minorIdx + 1 < structEnd) {
                            int major = ((adv[majorIdx] & 0xFF) << 8) | (adv[majorIdx+1] & 0xFF);
                            int minor = ((adv[minorIdx] & 0xFF) << 8) | (adv[minorIdx+1] & 0xFF);
                            return new int[]{major, minor};
                        }
                    }
                }
            }
            i += (1 + len);
        }
        return new int[]{-1,-1};
    }

    // ---------- Helpers ----------
    private static String bytesToHex(byte[] data) {
        if (data == null) return "";
        StringBuilder sb = new StringBuilder(data.length * 3);
        for (byte b : data) sb.append(String.format("%02X:", b));
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private boolean permisosOk() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void pedirPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT },
                    REQ_PERMISOS);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    REQ_PERMISOS);
        }
    }

    private boolean precondicionesOk() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) { toast("Sin Bluetooth"); return false; }
        if (!permisosOk()) { pedirPermisos(); return false; }
        if (!bta.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_BT_ON);
            return false;
        }
        if (escaner == null) {
            escaner = bta.getBluetoothLeScanner();
            if (escaner == null) { toast("Escáner LE no disponible"); return false; }
        }
        return true;
    }

    private String obtenerNombreSeguro(BluetoothDevice d) {
        if (d == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                return d.getName();
            } else return null;
        } else {
            return d.getName();
        }
    }

    // ---------- Permisos / resultados ----------
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISOS) {
            boolean ok = true;
            for (int r : grantResults) ok &= (r == PackageManager.PERMISSION_GRANTED);
            if (ok) inicializarBluetooth();
            else log("Permisos denegados");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_BT_ON) inicializarBluetooth();
    }

    private void toast(String s) { runOnUiThread(() -> Toast.makeText(this, s, Toast.LENGTH_SHORT).show()); }
    private void log(String s)   { Log.d(TAG, s); }
}
