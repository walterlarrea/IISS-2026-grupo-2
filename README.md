# EcoWarm — Controlador de temperatura (Grupo 2) · Versión 6

Controlador automático de calefacción por habitación (losa radiante) que recibe lecturas de termostatos por MQTT,
decide qué switches encender respetando la potencia contratada y la tarifa punta, y los comanda por REST.
Este README es el punto de entrada para el equipo que continúa el desarrollo (estado al tag `version6`).

## 0. PARA EL EQUIPO RECEPTOR

### Requisitos
- JDK 25 y Maven 3.9+.
- Docker con Docker Compose (también necesario para los tests de integración, que usan Testcontainers).
- Bash para los scripts (en Windows: Git Bash).

### Primeros pasos
```bash
mvn -B verify
```
```bash
bash scripts/build.sh dev
```
```bash
bash scripts/up.sh
```
Luego abrir Swagger en <http://localhost:8080/swagger-ui.html>, cargar un sitio con `PUT /sitio`
(`scripts/curl-ejemplos/curl-site/site-replace.sh`) e iniciar el control con `POST /control/comandos`
(`scripts/curl-ejemplos/curl-ctrl/ctrln-start.sh`). Todas las llamadas llevan el header `X-API-Key`.

### Documentación
| Documento | Contenido |
|---|---|
| [docs/arquitectura/4mas1.md](docs/arquitectura/4mas1.md) | Arquitectura 4+1 (vistas lógica, procesos, desarrollo, física y escenarios). |
| [docs/arquitectura/decisiones.md](docs/arquitectura/decisiones.md) | Decisiones de arquitectura (ADR) y su porqué. |
| [docs/arquitectura/roadmap.md](docs/arquitectura/roadmap.md) | Limitaciones conocidas, problemas conocidos y roadmap. |
| [CHANGELOG.md](CHANGELOG.md) | Cambios por versión. |
| [docs/api/](docs/api/) | Contratos OpenAPI del controlador y del simulador (interop-v2). |
| [docs/calidad/estandares-codificacion.md](docs/calidad/estandares-codificacion.md) | Estándares de código. |
| [docs/metodologia.md](docs/metodologia.md), [docs/vision.md](docs/vision.md), [docs/producto/](docs/producto/) | Metodología, visión, escenarios e historias del producto. |
| [docs/mcp/rest-vs-mcp.md](docs/mcp/rest-vs-mcp.md) | Comparación REST vs MCP. |
| [.github/integration-tests/](.github/integration-tests/) | Plan y resumen de las pruebas de integración. |

### Configuración del algoritmo de control
Variables de entorno (en `.env.dev` / `.env.local`, pasadas por `docker/docker-compose.yml`):

| Variable | Default | Efecto |
|---|---|---|
| `CONTROLADOR_HISTERESIS_ENCENDIDO_C` | `0.3` | °C bajo el objetivo para encender una habitación apagada. `0` la desactiva. |
| `CONTROLADOR_INERCIA_TERMICA_C` | `0.1` | Banda en la que una habitación encendida cede potencia a otra más fría. |
| `CONTROLADOR_ESPERA_MAXIMA_MIN` | `60` | Minutos de espera tras los cuales una habitación pasa primero (anti-starvation). |
| `CONTROLADOR_MINIMO_ENCENDIDA_MIN` | `10` | Minutos que una habitación recién encendida no puede ser desalojada. |
| `REGISTRATION_RETRY_MS` | `5000` | Período de reevaluación del control y de reintento de registro en el simulador. |
| `CONECTAR_CON_SIMULADOR` | `true` | Registra el controlador en el simulador al iniciar. |
| `SIMULATOR_API_URL` / `SIMULATOR_API_KEY` | — | URL y clave del simulador. |

La antigüedad máxima de lecturas (`controlador.lectura-max-antiguedad-min`, 15 min) se configura como propiedad
de Spring. Todos los plazos se miden con el reloj del control (acelerado por `factorTiempo`).

## 1. ESTRUCTURA

El proyecto está organizado en módulos Maven, cada uno con una responsabilidad específica dentro del sistema de control
de temperatura EcoWarm.

### Directorios principales

**controller/** _(o engine)_ contiene la lógica de integración y ejecución del controlador, la recepción de mediciones y las API
relacionadas con la gestión del sistema.

**core/** contiene la lógica central del control de temperatura y las reglas de negocio, separadas de las tecnologías
externas.

**publisher/** genera y publica mediciones de temperatura mediante MQTT para simular los sensores.

**switches-api/** expone la API REST para consultar y modificar el estado de los switches.

**common/** reúne funcionalidades compartidas que pueden utilizar los distintos módulos.

**mcp-server/** servidor MCP (Spring AI) que expone herramientas sobre la API REST del controlador para asistentes de IA.

> Los módulos Maven están en la raíz del repositorio (equivalen a `/modulos/` del enunciado).

**docker/** contiene la configuración necesaria para construir y ejecutar los servicios mediante Docker Compose.

**scripts/** contiene scripts para construir, iniciar y detener los servicios, probar las API y enviar o recibir
mensajes MQTT.

El proyecto utiliza **Spring Boot** para las aplicaciones Java, **MQTT** para la comunicación entre componentes, **MongoDB** para persistir los datos y **Docker Compose** para ejecutar los servicios.

El archivo **pom.xml** raíz agrega los módulos Maven del proyecto, mientras que cada módulo dispone de su propia
configuración.

## 2. SCRIPTS y CONF

1. Ejecuta `docker --help` en la consola para consultar los comandos disponibles.
2. Desde la raíz del proyecto, ejecuta los scripts `.sh` con Bash. Por ejemplo: `bash scripts/up.sh`.
3. Para construir las imágenes con un entorno específico, usa `bash scripts/build.sh local` (para `.env.local`) o `bash scripts/build.sh dev` (para `.env.dev`). Si no se indica un entorno, se usa `dev` por defecto.

**#!/bin/bash --> se tiene que ejecutar usando Bash**

- **Scripts docker**
  - **up.sh** sirve para levantar Docker compose y crear la imagen antes de iniciar los contenedores.
  - **down.sh** sirve para detener y eliminar los contenedores.
  - **stop.sh** sirve para detener pero sin eliminar los contenedores.
  - **build.sh** sirve para crear la imagen en Docker.
  - **receive-temp.sh** se suscribe a un tópico MQTT y muestra en la consola todos los mensajes que lleguen de dicho
    tópico.
  - **send-temp.sh** publica los mensajes en un tópico MQTT y estos le llegan a quien esté suscripto.
  - **test-validacion.sh** para crear datos de prueba (validos e invalidos) a traves de la API de habitaciones
  - **run-controlador-local.sh** para cambiarlo a local.
  - **kill-ports.sh** busca procesos en esos puertos y lo mata (Bash).
  - **kill-ports.ps1** busca procesos en esos puertos y lo mata (PowerShell).
- **Simulación** (`scripts/simulacion/`)
  - **build-sim.sh** sirve para crear la imagen del simulador en Docker.
  - **up-sim.sh** sirve para levantar el simulador en Docker.
- **curl-ejemplos** (`scripts/curl-ejemplos/`)
  - **curl-ctrl**
    - **ctrln-start.sh** inicia el controlador. Modificar el factor tiempo a gusto.
    - **ctrln-status.sh** obtiene el estado del controlador.
    - **ctrln-stop.sh** detiene el controlador.

  - **curl-rooms**
    - **rooms-create0.sh** crea la habitación nº0.
    - **rooms-create1.sh** crea la habitación nº1.
    - **rooms-delete.sh** elimina una habitación por su ID.
    - **rooms-find.sh** busca la habitación por ID de termostato.
    - **rooms-list.sh** lista todas las habitaciones.
    - **rooms-patch.sh** modificar la temperatura esperada de la habitación por su ID.
    - **rooms-validar.sh** validar todas las habitaciones de la coleccion (detecta inválidas o duplicadas).

  - **curl-site**
    - **site-replace.sh** reemplaza el inventory.
    - **site-status.sh** obtiene el inventory del site.

  - **curl-switch**
    - **switch-off.sh** apaga el switch.
    - **switch-on.sh** enciende el switch.
    - **switch-status-sw0.sh** consulta el estado actual del switch 0.

## 3. EJECUCIÓN

**Como ejecutar el programa en Java**

1. Abrir el proyecto desde su IDE.
2. Asegurarse de que este en JDK-25.
3. Ejecutar el programa desde Compose.
4. El programa se conectará al broker MQTT.
5. Una vez conectado, se suscribirá a los tópicos MQTT configurados para las habitaciones del inventario activo. Si no hay habitaciones configuradas, no se suscribirá a ningún tópico.
6. Cualquier mensaje recibido lo mostrará en la consola.

### Integración con el simulador EcoWarm

Iniciar el Simulador con el argumento `host` setteado en la ruta de salida del docker

```
java -jar ecowarm-run.jar --host host.docker.internal
```

**IMPORTANTE**

**Para cuando se usa Compose debug (actual) y no Compose Sim (Simulador), cambiar las variables de entorno (ubicado en
.env) a false (true si usa el Simulador):**

**CONECTAR_CON_SIMULADOR=false**

**Datos de conexión necesarios**

**broker**: tcp://localhost:1883 **(esto en caso de que sea una conexión únicamente local, sino: tcp://mosquitto:1883)**

**clientId**: JavaSubscriber **(local, si es dev: JavaSubscriberDev)**

**topics**: los configurados para las habitaciones del inventario activo.

## 4. MÓDULO: PUBLISHER

**Contiene un generador de eventos aleatorios con diferentes tópicos para sus respectivas habitaciones.**\
Este manda mensajes al broker cada 3 segundos, con una id del dispositivo que lo envía, su temperatura y la fecha de
envío como Epoch en milisegundos. Está desarrollada utilizando Spring Boot.

## 5. MÓDULO: CONTROLLER (API + ENGINE)

**Se juntaron los módulos subscriber y rooms-api en uno solo.**

El módulo controller integra los componentes necesarios para ejecutar el sistema de control automático de temperatura.
Actúa como intermediario entre las mediciones recibidas, la configuración del site, la lógica de control del core y los
dispositivos que se deben accionar.

Está desarrollado en Java con Spring Boot y utiliza MQTT, MongoDB y API REST para comunicarse con los demás componentes.

### Endpoints principales

- **EngineController**
  - GET /control: consulta y devuelve el estado actual del controlador.
  - POST /control/comandos: permite iniciar o detener el controlador.

- **RoomController**
  - GET /habitaciones: obtiene todas las habitaciones.
  - GET /habitaciones/{id}: obtiene una habitación por su id.
  - GET /habitaciones/termostato/{idTermostato}: obtiene la habitación asociada a un termostato.
  - GET /habitaciones/validar: valida las habitaciones registradas.
  - POST /habitaciones: crea una nueva habitación.
  - PUT /habitaciones/{id}: modifica una habitación existente.
  - PATCH /habitaciones/{id}: modifica parcialmente una habitación.
  - DELETE /habitaciones/{id}: elimina una habitación.

- **SiteController**
  - GET /sitio: consulta la configuración actual del site.
  - PUT /sitio: reemplaza y actualiza el estado del site.

## 6. MÓDULO: CORE

El core recibe la información necesaria para evaluar las condiciones del sistema y determinar las acciones que
corresponden según las reglas configuradas. El engine integra esta lógica con las mediciones reales, el estado de las
habitaciones y la comunicación con los switches.

### Responsabilidades principales

- **Evaluación de temperaturas** permite determinar las acciones de control a partir de las temperaturas actuales y
  esperadas.

- **Reglas de negocio** concentra las decisiones del sistema sin depender directamente de MQTT, MongoDB o los
  controladores REST.

- **Evaluación de condiciones de operación** permite modelar las reglas que correspondan al funcionamiento del sistema,
  como las relacionadas con las tarifas, horarios rushHour, etc.

Esta separación permite mantener el core enfocado en las decisiones del sistema y el engine encargado de integrarlas con
el entorno real.

## 7. MÓDULO: SWITCHES-API

**Contiene la API REST encargada de consultar y modificar el estado de los switches del sistema.**\
Está desarrollada utilizando Spring Boot.
El módulo es utilizado por el controlador automático de temperatura para activar o desactivar el switch correspondiente
según la temperatura actual de una habitación.

- **Endpoints principales**
  - GET /switches/{id}: obtiene el estado actual de un switch.
  - POST /switches/{id}/comandos: actualiza el estado del switch, permitiendo encenderlo o apagarlo.

## 8. SWAGGER/OPENAPI

Swagger permite visualizar y probar los endpoints disponibles de cada API desde una interfaz web, mostrando los métodos
HTTP, parámetros, cuerpos de las solicitudes y respuestas esperadas.

Es una alternativa más sencilla y fácil de visualizar que los ejemplos curl.

## 9. TEST UNITARIOS

Se crearon test para probar tramos del programa y confirmar el funcionamiento de envío y recibimiento de mensajes y
datos.

```bash
mvn clean test
```

**controller**

- **controller/api**
- **controller/engine**
- **controller/integration**

**core**

**publisher**

**switches-api**

## 10. TEST DE INTEGRACIÓN

Se crearon test para probar tramos del programa y confirmar el funcionamiento de envío y recibimiento de mensajes y
datos.
Todos los test que terminan en IT son las pruebas de integración.

> Corre la suite entera de Tests Unitarios + Integración

```bash
mvn -B verify
```

Requiere Docker activo. Detalle de cada prueba en
[.github/integration-tests/integration-test-summary.md](.github/integration-tests/integration-test-summary.md).

## 11. MONGO DB

Se creó una base de datos llamada **Sensores**. Entre sus colecciones se encuentran:

- **inventario_controlador**: contiene el inventario activo del sitio y sus habitaciones.
- **habitaciones**: contiene los documentos de las habitaciones.
- **Mediciones**: recibe las mediciones de temperatura. El nombre puede configurarse mediante
  `MONGODB_COLLECTION_MEDICIONES`.

## 12. JIRA

Acá se encuentra la organización y seguimiento del proyecto.

[Link a Jira](https://walterlarrea.atlassian.net/jira/software/projects/SCRUM/boards/1?filter=&groupBy=none&atlOrigin=eyJpIjoiYjAzY2VmYzE5OTgzNDFiNjgyNDEzM2Y2OTA2ODI2NjgiLCJwIjoiaiJ9)

## 13. MCP Server

Para ejecutar el MCP Server:

```bash
docker compose --env-file .env.dev -f docker/docker-compose.yml up -d --build mcp-server
```

Config para Claude

```json
{
  "mcpServers": {
    "controlador-sitio": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8081/mcp"]
    }
  }
}
```
