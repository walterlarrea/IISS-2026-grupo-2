# Plan de pruebas de integración

## Alcance y cobertura existente

El módulo `controlador` ya tiene pruebas unitarias de `MqttSubscriber`/suscripciones y de
`SwitchClient`, pero esas pruebas no ejercitan un broker MQTT ni una comunicación HTTP
real. Estas pruebas cubren los límites de red de esos componentes mediante Mosquitto y un
servidor HTTP local.

## Estrategia

Se usa la capa 1 (integración local): Mosquitto corre en Testcontainers; el publicador
Paho y `MqttSubscriber` intercambian mensajes reales. La comunicación REST se prueba con
un servidor HTTP local y `SwitchClient` con un `RestClient` real. Los repositorios y el
controlador de temperatura se sustituyen por colaboradores aislados porque el objetivo
es validar las fronteras MQTT y REST, no MongoDB ni la lógica de control.

## Escenarios y criterios

| Componente | Escenario | Criterio |
| --- | --- | --- |
| MQTT | El broker se detiene y vuelve a iniciar | El suscriptor restablece su suscripción y persiste una nueva medición. |
| MQTT | Mensaje QoS 1 publicado con el suscriptor desconectado | El mensaje no se reproduce al reconectar con sesión limpia; el mensaje posterior sí llega. |
| REST | La API remota está fuera de línea | `SwitchClient` expone el fallo de conexión. |
| REST | La API remota responde HTTP 503 | `SwitchClient` transmite el fallo HTTP y se confirma que la solicitud llegó al servidor. |

## Dependencias y ejecución

- Java 25 y Maven.
- Docker en ejecución para los escenarios MQTT.
- Testcontainers JUnit Jupiter y la imagen `eclipse-mosquitto:2.0.22`.
- Desde la raíz, compilar e instalar las dependencias del módulo sin ejecutar sus
  pruebas y después filtrar por etiqueta en el módulo que las contiene:
  `mvn -pl controlador -am -DskipTests install` y
  `mvn -pl controlador verify -Dgroups=Layer1`.
