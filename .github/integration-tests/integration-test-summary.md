# Resumen de pruebas de integración

## Pruebas de capa 1

- `controlador/src/test/java/com/iiss/sensores/grupo_2/controlador/integration/BigBangSystemIntegrationIT.java`
  - Prueba end-to-end del controlador, API de switches y broker MQTT.
- `controlador/src/test/java/com/iiss/sensores/grupo_2/controlador/integration/MqttResilienceIT.java`
  - Comprueba recuperación de suscripción tras caída/reinicio de Mosquitto.
  - Comprueba pérdida de mensajes publicados mientras el suscriptor está desconectado y entrega de mensajes posteriores.
- `controlador/src/test/java/com/iiss/sensores/grupo_2/controlador/integration/SwitchClientRestFailureIT.java`
  - Comprueba que `SwitchClient` propaga fallos de conexión y HTTP 503 desde una API local real.
  - Comprueba que, si el comando REST falla dentro de `ControllerTempAuto`, se conserva el último estado local conocido.
- `controlador/src/test/java/com/iiss/sensores/grupo_2/controlador/integration/TemperatureReadingRepositoryIT.java`
  - Usa MongoDB real para guardar y recuperar las últimas lecturas por `idThermostat`.
- `controlador/src/test/resources/mosquitto.conf`
  - Configuración anónima y logs necesarios para el broker de prueba.

## Cobertura y dependencias

Se comprueba además la persistencia real en MongoDB y el orden temporal por termostato.
Las siete pruebas de integración se descubren mediante Maven Failsafe por el sufijo
`IT`. Usan Testcontainers para MQTT y MongoDB o un servidor HTTP local para REST; Docker
debe estar activo para ejecutarlas.

## Resultado

Validación con la convención Failsafe:

- Surefire: `mvn -pl controlador test` ejecutó 55 pruebas unitarias, todas pasaron.
- Failsafe: `mvn -pl controlador failsafe:integration-test failsafe:verify` ejecutó
  7 pruebas de integración, todas pasaron.
- `mvn -pl controlador verify` ejecutó las 55 pruebas unitarias y las 7 de integración,
  todas pasaron en una sola ejecución con Docker activo.
- Se corrigió el classpath de Failsafe para que use `target/classes`; el reempaquetado
  ejecutable de Spring Boot hacía que tomara el JAR repaquetado y no encontrara las
  clases de producción al descubrir los IT.

## Runner

- Windows: `powershell .github/integration-tests/run-layer1-tests.ps1`
- Unix: `bash .github/integration-tests/run-layer1-tests.sh`
- Comando Maven para todas las pruebas del controlador: `mvn -pl controlador verify`.

## Cambios de producción

`TemperatureReadingRepository` usa `idThermostat` de forma consistente para guardar,
indexar, filtrar y agrupar las lecturas; convierte `Instant` a BSON Date al guardar y
reconstruye el `Instant` al leer.
