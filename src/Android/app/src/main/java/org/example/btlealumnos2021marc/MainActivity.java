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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Flujo AUTO-GUARDAR (solo MINOR):
 * 1) Botón "Buscar nuestro dispositivo": filtra por nombre EXACTO "Grupo4".
 * 2) Al primer iBeacon Apple (4C 00 02 15): extrae MINOR (big-endian), guarda y detiene.
 * 3) Se ignora completamente el MAJOR.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BEACON";
    private static final int REQ_PERMISOS = 11223344;
    private static final int REQ_BT_ON    = 778899;

    private static final String NOMBRE_BEACON = "Grupo4";

    private BluetoothLeScanner escaner;
    private ScanCallback callback;
    private LogicaFake logicaFake;

    // Para evitar duplicados en una misma sesión de escaneo
    private volatile boolean enviado = false;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logicaFake = new LogicaFake();
        inicializarBluetooth();
    }

    // ---------- Botones ----------
    public void botonBuscarDispositivosBTLEPulsado(View v) { iniciarEscaneoGeneral(); }
    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) { iniciarEscaneoNuestro(); }
    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) { detener(); }

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

    // ---------- Escaneo “nuestro”: filtra por nombre y auto-guarda SOLO MINOR ----------
    private void iniciarEscaneoNuestro() {
        log("Escaneo nuestro: buscando \"" + NOMBRE_BEACON + "\"");
        if (!precondicionesOk()) return;
        enviado = false;

        List<ScanFilter> filtros = new ArrayList<>();
        filtros.add(new ScanFilter.Builder().setDeviceName(NOMBRE_BEACON).build());

        callback = new ScanCallback() {
            @Override public void onScanResult(int callbackType, ScanResult r) {
                if (enviado) return;
                if (r == null || r.getScanRecord() == null) return;

                String nombre = obtenerNombreSeguro(r.getDevice());
                log("Visto: " + (nombre != null ? nombre : "(sin nombre)") + " · RSSI=" + r.getRssi());

                byte[] adv = r.getScanRecord().getBytes();
                if (adv == null) return;

                int valor = extractMinorFromIBeacon(adv); // <-- parseo robusto (Manufacturer Data)
                if (valor >= 0) {
                    log("iBeacon OK → minor(valor) = " + valor);

                    // Guardar SOLO el valor (minor). La BBDD autoincrementa el id.
                    logicaFake.guardarMedicion(valor);

                    enviado = true;
                    toast("Medición guardada: " + valor);
                    detener();
                } else {
                    // No era iBeacon Apple; para depurar, muestra el payload una vez
                    log("No iBeacon o formato inesperado. ADV(" + adv.length + ")=" + bytesToHex(adv));
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
            toast("Buscando \"" + NOMBRE_BEACON + "\"…");
        } else {
            pedirPermisos();
        }
    }

    // ---------- Escaneo general (depuración) ----------
    private void iniciarEscaneoGeneral() {
        log("Escaneo general: empieza");
        if (!precondicionesOk()) return;

        callback = new ScanCallback() {
            @Override public void onScanResult(int callbackType, ScanResult r) {
                if (r == null || r.getScanRecord() == null) return;
                String nombre = obtenerNombreSeguro(r.getDevice());
                Log.d(TAG, "***** DISPOSITIVO ***** nombre=" + nombre + " rssi=" + r.getRssi());
                byte[] adv = r.getScanRecord().getBytes();
                if (adv == null) return;

                int minor = extractMinorFromIBeacon(adv);
                if (minor >= 0) {
                    Log.d(TAG, "iBeacon → minor(valor)=" + minor);
                } else {
                    Log.d(TAG, "No iBeacon. ADV(" + adv.length + ")=" + bytesToHex(adv));
                }
            }
        };

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            escaner.startScan(null, settings, callback);
            toast("Escaneo BLE iniciado (general)");
        } else {
            pedirPermisos();
        }
    }

    private void detener() {
        if (callback == null || escaner == null) { toast("Escaneo detenido"); return; }
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                escaner.stopScan(callback);
            }
        } catch (Exception ignored) {}
        callback = null;
        toast("Escaneo detenido");
    }

    // ---------- Parseo robusto: extrae MINOR (big-endian) desde Manufacturer Data (0xFF) ----------
    // iBeacon Apple = 4C 00 02 15 + UUID(16) + Major(2) + Minor(2) + TxPower(1)
    private static int extractMinorFromIBeacon(byte[] adv) {
        if (adv == null || adv.length < 30) return -1;

        int i = 0;
        while (i < adv.length) {
            int len = adv[i] & 0xFF;
            if (len == 0) break;                  // fin de estructuras
            int typeIdx = i + 1;
            int dataIdx = i + 2;
            if (typeIdx >= adv.length) break;
            int type = adv[typeIdx] & 0xFF;

            if (type == 0xFF) { // Manufacturer Specific Data
                int structEnd = i + 1 + len; // índice exclusivo (después de esta estructura)
                int base = dataIdx;          // inicio de los datos (company ID va aquí)

                // Comprobar cabecera iBeacon y espacio suficiente
                if (base + 4 <= structEnd) {
                    boolean isAppleIBeacon =
                            (adv[base]     == (byte)0x4C) &&
                                    (adv[base + 1] == (byte)0x00) &&
                                    (adv[base + 2] == (byte)0x02) &&
                                    (adv[base + 3] == (byte)0x15);
                    if (isAppleIBeacon) {
                        // Offset del minor dentro del Manufacturer Data:
                        // 4 (cabecera) + 16 (UUID) + 2 (Major) = 22
                        int minorIdx = base + 4 + 16 + 2;
                        // Confirmar que minor (2 bytes) cabe dentro de la estructura
                        if (minorIdx + 1 < structEnd) {
                            int minor = ((adv[minorIdx] & 0xFF) << 8) | (adv[minorIdx + 1] & 0xFF); // BIG-ENDIAN
                            return minor;
                        }
                    }
                }
            }
            i += (1 + len); // avanzar a la siguiente AD structure
        }
        return -1;
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
