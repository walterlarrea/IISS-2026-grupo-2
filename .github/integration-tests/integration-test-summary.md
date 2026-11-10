# Resumen de pruebas de integración

Estado al cierre de la Iteración 6. Todas las pruebas de integración están en
`controller/src/test/java/com/iiss/sensores/grupo_2/controlador/integration/` y terminan en `IT`
(las descubre Maven Failsafe).

## Pruebas

| Clase | Prueba | Qué valida | Infraestructura |
|---|---|---|---|
| `BigBangSystemIntegrationIT` | `fullSystemEndToEndFlowIT` | Flujo end-to-end: sitio, lecturas MQTT, decisión y comandos a switches. | Testcontainers (MQTT, MongoDB) |
| | `notEnoughPowerToTurnOnAllSwitchesIT` | Con potencia insuficiente solo se encienden las habitaciones que entran en la contratada. | Testcontainers |
| `MqttResilienceIT` | `subscriberReconnectsAndReceivesMessagesAfterBrokerRestart` | Reconexión y re-suscripción tras caída/reinicio de Mosquitto. | Testcontainers (Mosquitto) |
| | `messagesPublishedWhileSubscriberIsOfflineAreLostButNewMessagesArrive` | Los mensajes publicados con el suscriptor desconectado se pierden; los posteriores llegan. | Testcontainers (Mosquitto) |
| `SwitchClientRestFailureIT` | `reportsConnectionFailureWhenRemoteSwitchApiIsOffline` | `SwitchClient` propaga fallos de conexión. | Servidor HTTP local |
| | `reportsServiceUnavailableWhenRemoteSwitchApiFails` | `SwitchClient` propaga HTTP 503. | Servidor HTTP local |
| | `keepsLastKnownSwitchStateWhenControllerCommandCannotReachSwitch` | Si el comando falla dentro de `TemperatureControlEngine`, se conserva el último estado conocido. | Servidor HTTP local |
| `TemperatureControlEngineIntegrationIT` | `controlTemperatureWhenEnoughPower` | Ciclo de control con potencia suficiente contra un switch simulado con estado (el GET de confirmación devuelve lo que dejó el último comando). | WireMock |
| `TemperatureReadingRepositoryIT` | `savesAndReturnsLatestTemperatureForEachThermostat` | Guarda y recupera la última lectura por `idThermostat` en MongoDB real. | Testcontainers (MongoDB) |

`controller/src/test/resources/mosquitto.conf` tiene la configuración anónima del broker de prueba.

Los escenarios de falla de la Iteración 6 (F03 termostato sin lecturas, F05 OFF que no apaga, F06/F09 OFF fallido y
potencia contratada, backoff en tiempo simulado) se cubren con pruebas unitarias del controlador
(`TemperatureControlEngineTest`, `SiteStateServiceTest`, `SwitchClientTest`, `CustomClockTest`). Ampliarlos a
pruebas de integración figura en el [roadmap](../../docs/arquitectura/roadmap.md) (P7).

## Ejecución

Docker debe estar activo.

- Todo el proyecto (unitarias + integración): `mvn -B verify`
- Solo el controlador: `mvn -pl controller -am verify`
- Solo integración: `mvn -pl controller -am -DskipTests install` y luego
  `mvn -pl controller failsafe:integration-test failsafe:verify`

## Notas técnicas

- El classpath de Failsafe usa `target/classes`: el reempaquetado ejecutable de Spring Boot hacía que tomara el JAR
  repaquetado y no encontrara las clases de producción.
- `TemperatureReadingRepository` usa `idThermostat` de forma consistente para guardar, indexar, filtrar y agrupar las
  lecturas; convierte `Instant` a BSON Date al guardar y lo reconstruye al leer.
