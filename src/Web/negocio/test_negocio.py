import medicion_service
import medicion_fake

# Insertar una medición de prueba
medicion_service.insertar_medicion(30.5)
print("Última medición real:", medicion_service.get_ultima_medicion())

# Medición fake
print("Medición fake:", medicion_fake.get_ultima_medicion())
