import Web.negocio.logica as logica

# Insertamos una medición de prueba
print("Insertando medición de prueba...")
logica.insertar_medicion(30.5)

# Obtenemos la última medición registrada
ultima = logica.get_ultima_medicion()

if ultima is not None:
    print("Última medición registrada en BD:", ultima)
else:
    print("No hay mediciones registradas.")

# Simulación de medición fake directamente en el test (sin archivo aparte)
medicion_fake = (999, 22.53, '2025-10-02 12:29:40')
print("Medición fake (simulada en test):", medicion_fake)
