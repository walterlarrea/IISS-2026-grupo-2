# Resumen de pruebas de integración

## Pruebas agregadas

- Se agregaron cuatro pruebas de capa 1 en dos clases etiquetadas con `Layer1`.
- `controlador/src/test/java/com/iiss/sensores/grupo_2/controlador/integration/MqttResilienceL1Test.java`
  - Comprueba recuperación de suscripción tras caída/reinicio de Mosquitto.
  - Comprueba pérdida de mensajes publicados mientras el suscriptor está desconectado y entrega de mensajes posteriores.
- `controlador/src/test/java/com/iiss/sensores/grupo_2/controlador/integration/SwitchClientRestFailureL1Test.java`
  - Comprueba que `SwitchClient` propaga fallos de conexión y HTTP 503 desde una API local real.
- `controlador/src/test/resources/mosquitto.conf`
  - Configuración anónima y logs necesarios para el broker de prueba.

## Cobertura y dependencias

Se agregan pruebas de red real en los límites MQTT y REST, que no estaban cubiertos por
las pruebas unitarias anteriores. Se incorpora Testcontainers JUnit Jupiter; Docker es
necesario para correr las pruebas MQTT.

## Resultado

El baseline previo a estos cambios pasó: 55 pruebas ejecutadas, 0 fallidas, 0 errores.
La ejecución filtrada descubrió las cuatro pruebas: las dos REST pasaron y las dos MQTT
terminaron con error porque el daemon de Docker no está disponible. No se reportaron
fallos de aserción. Las pruebas MQTT compilaron correctamente.

## Runner

- Windows: `powershell .github/integration-tests/run-layer1-tests.ps1`
- Unix: `bash .github/integration-tests/run-layer1-tests.sh`
- Comandos Maven: `mvn -pl controlador -am -DskipTests install` y luego
  `mvn -pl controlador verify -Dgroups=Layer1`.

## Cambios de producción

No se modificó código de producción.
