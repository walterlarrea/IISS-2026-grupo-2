# Resumen de pruebas de integración

## Pruebas de capa 1

- `controlador/src/test/java/com/iiss/sensores/grupo_2/controlador/integration/MqttResilienceL1Test.java`
  - Comprueba recuperación de suscripción tras caída/reinicio de Mosquitto.
  - Comprueba pérdida de mensajes publicados mientras el suscriptor está desconectado y entrega de mensajes posteriores.
- `controlador/src/test/java/com/iiss/sensores/grupo_2/controlador/integration/SwitchClientRestFailureL1Test.java`
  - Comprueba que `SwitchClient` propaga fallos de conexión y HTTP 503 desde una API local real.
- `controlador/src/test/java/com/iiss/sensores/grupo_2/controlador/integration/TemperatureReadingRepositoryL1Test.java`
  - Usa MongoDB real para guardar y recuperar las últimas lecturas por `idThermostat`.
- `controlador/src/test/resources/mosquitto.conf`
  - Configuración anónima y logs necesarios para el broker de prueba.

## Cobertura y dependencias

Se comprueba además la persistencia real en MongoDB y el orden temporal por termostato.
Las cinco pruebas de capa 1 usan Testcontainers para MQTT y MongoDB o un servidor HTTP
local para REST; Docker debe estar activo para ejecutarlas.

## Resultado

La última ejecución de capa 1 pasó: 5 pruebas, 0 fallidas, 0 errores (2 MQTT, 2 REST y
1 MongoDB).

## Runner

- Windows: `powershell .github/integration-tests/run-layer1-tests.ps1`
- Unix: `bash .github/integration-tests/run-layer1-tests.sh`
- Comandos Maven: `mvn -pl controlador -am -DskipTests install` y luego
  `mvn -pl controlador verify -Dgroups=Layer1`.

## Cambios de producción

`TemperatureReadingRepository` usa `idThermostat` de forma consistente para guardar,
indexar, filtrar y agrupar las lecturas; convierte `Instant` a BSON Date al guardar y
reconstruye el `Instant` al leer.
