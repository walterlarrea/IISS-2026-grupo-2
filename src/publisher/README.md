## Resumen del módulo Publisher

El módulo Publisher simula sensores de temperatura que publican datos a un broker MQTT para que otros componentes del sistema los consuman.

### ¿Qué hace?

- Genera valores de temperatura para varios “termostatos” simulados.
- Cada uno publica mensajes JSON cada 3 segundos.
- Los mensajes se envían a topics MQTT como:
  - ht-sim-room0/status/temperature:0
  - ht-sim-room1/status/temperature:1
- La temperatura cambia gradualmente y se mantiene dentro de un rango seguro: de -2°C a 37°C.

### ¿Cómo funciona?

- El servicio se inicia con Spring Boot.
- Lee la URL del broker desde una variable de entorno: MQTT_BROKER_URL.
- Crea un cliente MQTT y un publicador por cada topic.
- En cada ciclo:
  1. calcula una nueva temperatura,
  2. arma un JSON,
  3. lo publica en el broker.

### Estructura del mensaje

Cada publicación tiene este formato:

```json
{
	"id": 0,
	"tC": 23.84,
	"tF": 0.0,
	"ts": 1778712345.123
}
```

- id: identificador del termostato
- tC: temperatura en °C
- tF: temperatura en °F (actualmente 0.0)
- ts: marca de tiempo

### Configuración

- Requiere Java 25.
- Necesita la variable MQTT_BROKER_URL.
- Ejemplo:
  - tcp://localhost:1883
  - tcp://mosquitto:1883

Se puede ejecutar junto con el broker usando Docker Compose.

### En pocas palabras

Es un generador de datos de prueba para IoT: simula habitaciones con sensores y publica temperaturas falsas en MQTT para que luego el subscriber las procese y guarde.

Si quieres, también te lo puedo convertir en una versión todavía más breve, tipo “README de 5 líneas” o “resumen para presentación”.
