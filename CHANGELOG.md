[//]: # 'Agrega las nuevas entradas al principio e incrementa la versión junto con el módulo; por ejemplo: 0.1.0, 0.1.1, 0.1.2, etc.'

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
