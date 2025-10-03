import datetime
import random

def get_ultima_medicion():
    """Devuelve una medición falsa (fake)"""
    return (
        999,                          # id inventado
        round(random.uniform(20, 30), 2),  # valor inventado
        datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")  # fecha actual
    )
