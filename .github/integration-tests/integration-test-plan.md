# Plan de pruebas de integración

## Alcance y cobertura existente

El módulo `controlador` ya tiene pruebas unitarias de `MqttSubscriber`/suscripciones y de
`SwitchClient`, pero esas pruebas no ejercitan un broker MQTT ni una comunicación HTTP
real. Estas pruebas cubren los límites de red mediante Mosquitto y un servidor HTTP local,
y comprueban el efecto de un fallo REST en el ciclo real de `ControllerTempAuto`.

## Estrategia

Se usa la capa 1 (integración local): Mosquitto corre en Testcontainers; el publicador
Paho y `MqttSubscriber` intercambian mensajes reales. La comunicación REST se prueba con
un servidor HTTP local y `SwitchClient` con un `RestClient` real. Un escenario envía un
comando mediante `ControllerTempAuto` y verifica que un HTTP 503 no modifica el estado
local conocido del switch. Un contenedor MongoDB verifica la persistencia y recuperación
de las últimas lecturas por `idThermostat`. El repositorio y el controlador de temperatura
se aíslan en los escenarios MQTT para concentrarse en la comunicación con el broker.

## Escenarios y criterios

| Componente | Escenario | Criterio |
| --- | --- | --- |
| MQTT | El broker se detiene y vuelve a iniciar | El suscriptor restablece su suscripción y persiste una nueva medición. |
| MQTT | Mensaje QoS 1 publicado con el suscriptor desconectado | El mensaje no se reproduce al reconectar con sesión limpia; el mensaje posterior sí llega. |
| MongoDB | Varias lecturas para el mismo termostato | Se devuelve la lectura más reciente por `idThermostat`; IDs desconocidos no generan resultados. |
| REST | La API remota está fuera de línea | `SwitchClient` expone el fallo de conexión. |
| REST | La API remota responde HTTP 503 | `SwitchClient` transmite el fallo HTTP y se confirma que la solicitud llegó al servidor. |
| Control automático | El switch falla al ejecutar el comando REST | `ControllerTempAuto` continúa el ciclo sin marcar localmente el comando como exitoso. |

## Dependencias y ejecución

- Java 25 y Maven.
- Docker en ejecución para los escenarios MQTT y MongoDB.
- Testcontainers JUnit Jupiter; imágenes `eclipse-mosquitto:2.0.22` y `mongo:8`.
- Las pruebas de integración usan el sufijo `IT` y Maven Failsafe; las pruebas unitarias
  permanecen bajo Surefire. Desde la raíz, instalar primero las dependencias sin
  ejecutar pruebas y después ejecutar solo las pruebas de integración:
  `mvn -pl controlador -am -DskipTests install` y
  `mvn -pl controlador failsafe:integration-test failsafe:verify`.
- Para ejecutar las pruebas unitarias: `mvn -pl controlador test`.
- Para ejecutar las pruebas unitarias y de integración juntas: `mvn -pl controlador verify`.
  Failsafe usa `target/classes` explícitamente para que el reempaquetado del JAR de
  Spring Boot no quite las clases del controlador de su classpath.
