[//]: # 'Agrega las nuevas entradas al principio e incrementa la versión junto con el módulo; por ejemplo: 0.1.0, 0.1.1, 0.1.2, etc.'

# 08/11/2026

## Corregido

- El controlador ya no enciende un switch si, sumando las habitaciones realmente encendidas, supera la potencia contratada. Un OFF fallido deja la habitación contando como encendida, así un error del switch no hace pasar la contratada (escenarios 5.1 F06/F09 y 5.2b F05).
- Cada OFF que responde OK se confirma con `GET /switches/{id}`: si el switch sigue en ON se trata como fallo y se reintenta. Si el GET no se puede hacer, se toma la respuesta del comando (escenario 5.2 F05).
- La antigüedad de la última lectura de cada termostato (`controlador.lectura-max-antiguedad-min`, 15 min por defecto) se mide con el reloj del control en lugar del reloj del sistema: con el reloj acelerado, un termostato que dejaba de publicar tardaba 15 minutos reales (15 horas simuladas con factor 60) en hacer apagar su switch (escenario 3 F03).
- El plazo para dejar de reintentar el apagado del switch de una habitación eliminada (1 h) también se mide con el reloj del control: con factor 60 eran 60 horas simuladas de reintentos.
- Al iniciar o detener el controlador, las marcas de tiempo ya registradas (última lectura de cada termostato y habitaciones eliminadas pendientes de apagar) se trasladan al nuevo reloj conservando el tiempo transcurrido: una fecha simulada anterior a la real ya no hace ver las lecturas como "del futuro" (que nunca vencían).
- El backoff de reintentos a switches se mide en tiempo simulado (30 s que se duplican hasta un tope de 2 min) en lugar de tiempo real: con el reloj acelerado un switch recuperado tardaba hasta 10 minutos simulados en volver a comandarse (escenarios 5.1 y 5.2b).

## Cambiado

- `TemperatureControlEngineIntegrationIT` simula un switch con estado (el GET devuelve lo que dejó el último comando).
- Documentación de arquitectura (`docs/arquitectura/4mas1.md`) actualizada con el chequeo de potencia, la confirmación de OFF, el backoff y el apagado de switches de termostatos sin lecturas.

# [2.1.1] 17/09/2026

## Cambiado

- Simplificación del workflow de GitHub Actions a un único job y un único paso Maven con `mvn -B verify` para compilar y ejecutar las pruebas.
- Incremento de la versión del proyecto a `2.1.1`.

# [2.1.0] 17/09/2026

## Añadido

- Configuración explícita de Mosquitto para habilitar conexiones MQTT en el puerto 1883 y WebSocket en el puerto 9001 desde Docker.
- Configuración de CI para compilar el proyecto Maven.

## Cambiado

- Reorganización de los módulos `publisher`, `subscriber`, `rooms-api` y `switches-api` desde `src/` hacia la raíz del repositorio.
- Actualización del `pom.xml` raíz y de los POM de los módulos para reflejar la nueva estructura y las versiones específicas de las dependencias.
- Actualización de Docker y Docker Compose para usar Java 25, una versión específica de Mosquitto y los artefactos generados por cada módulo.
- Actualización de la documentación, las configuraciones de ejecución de IntelliJ y la configuración de lanzamiento de VS Code para la nueva estructura.

## Corregido

- Serialización de las marcas de tiempo de temperatura como valores `Long` enteros en milisegundos desde Epoch.
- Pruebas del publisher para validar la serialización de timestamps y la generación de eventos.

# [0.1.2] 11/08/2026

## Fixed

- Dockerfile to use Java 25

## Added

- Environment files for local and docker development environments
- Class to handle environment values
- Run configuration files for IntelliJ and VSCode

## Updated

- Docker compose to use environment variables from env
- /gitignore

# [0.1.1] 10/08/2026

## Changed

- mqtt service port from 1884 to 1883

# [0.1.0] 08/09/2026

## Added

- Initial Dockerfile for containerization

## Changed

- docker compose file to create a container with the Java project image
