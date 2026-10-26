# PROYECTO DE SENSORES VERSION 4.

## 1. ESTRUCTURA

El proyecto está organizado en módulos Maven, cada uno con una responsabilidad específica dentro del sistema de control de temperatura EcoWarm.

### Directorios principales

**engine/** contiene la lógica de integración y ejecución del controlador, la recepción de mediciones y las API relacionadas con la gestión del sistema.

**core/** contiene la lógica central del control de temperatura y las reglas de negocio, separadas de las tecnologías externas.

**publisher/** genera y publica mediciones de temperatura mediante MQTT para simular los sensores.

**switches-api/** expone la API REST para consultar y modificar el estado de los switches.

**common/** reúne funcionalidades compartidas que pueden utilizar los distintos módulos.

**docker/** contiene la configuración necesaria para construir y ejecutar los servicios mediante Docker Compose.

**scripts/** contiene scripts para construir, iniciar y detener los servicios, probar las API y enviar o recibir mensajes MQTT.

El proyecto utiliza **Spring Boot** para las aplicaciones Java, **MQTT** para la comunicación entre componentes, **MongoDB** para persistir los datos y **Docker Compose** para ejecutar los servicios.

El archivo **pom.xml** raíz agrega los módulos Maven del proyecto, mientras que cada módulo dispone de su propia configuración.

## 2. SCRIPTS y CONF

1. docker --help, en consola para ver sus respectivos commands.
2. Para ejecutar los scripts .sh en la consola. Ej: **./up.sh** o **bash up.sh** o **sh up.sh**
3. Para ejecutar el script build. sh, se puede usar: build.sh local (para .env.local) o build.sh dev (para .env.dev). Si no se escribe nada posterior al .sh, tomará a dev por defecto.

**#!/bin/bash --> se tiene que ejecutar usando Bash**
- **Scripts docker**
  - **up.sh** sirve para levantar Docker compose y crear la imagen antes de iniciar los contenedores.
  - **down.sh** sirve para detener y eliminar los contenedores.
  - **stop.sh** sirve para detener pero sin eliminar los contenedores.
  - **build.sh** sirve para crear la imagen en Docker.
  - **receive-temp.sh** se suscribe a un tópico MQTT y muestra en la consola todos los mensajes que lleguen de dicho tópico.
  - **send-temp.sh** publica los mensajes en un tópico MQTT y estos le llegan a quien esté suscripto.
  - **test-validacion.sh**  para crear datos de prueba (validos e invalidos) a traves de la API de habitaciones
  - **run-controlador-local.sh** para cambiarlo a local.
- **curl-ejemplos**

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
5. Una vez conectado se suscribirá al tópico : "#".
   - El '#' es para que reciba todos los mensajes que llegan a los tópicos del broker.
6. Cualquier mensaje recibido lo mostrará en la consola.

### Integración con el simulador EcoWarm

Iniciar el Simulador con el argumento `anunciar` setteado en la ruta de salida del docker
```
java -jar ecowarm-check.jar --anunciar host.docker.internal
```

**IMPORTANTE**

**Para cuando se usa Compose debug (actual) y no Compose Sim (Simulador), cambiar las variables de entorno (ubicado en .env) a false (true si usa el Simulador):**

**CONECTAR_CON_SIMULADOR=false**

**Datos de conexión necesarios**

**broker**: tcp://localhost:1883 **(esto en caso de que sea una conexión únicamente local, sino: tcp://mosquitto:1883)**

**clientId**: JavaSubscriber **(local, si es dev: JavaSubscriberDev)**

**topic**: "#"

## 4. MÓDULO: PUBLISHER

**Contiene un generador de eventos aleatorios con diferentes tópicos para sus respectivas habitaciones.**\
Este manda mensajes al broker cada 3 segundos, con una id del dispositivo que lo envía, su temperatura y la fecha de envío como Epoch en milisegundos. Está desarrollada utilizando Spring Boot.

## 5. MÓDULO: ENGINE

**Se juntaron los módulos subscriber y rooms-api en uno solo.**

El módulo engine integra los componentes necesarios para ejecutar el sistema de control automático de temperatura. Actúa como intermediario entre las mediciones recibidas, la configuración del site, la lógica de control del core y los dispositivos que se deben accionar.

Está desarrollado en Java con Spring Boot y utiliza MQTT, MongoDB y API REST para comunicarse con los demás componentes.
### Endpoints principales
- **EngineController**
  - GET /control: consulta y devuelve el estado actual del controlador.
  - POST /control/commands: permite iniciar o detener el controlador.

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
  - GET /site: consulta la configuración actual del site.
  - PUT /site: reemplaza y actualiza el estado del site.

## 6. MÓDULO: CORE

El core recibe la información necesaria para evaluar las condiciones del sistema y determinar las acciones que corresponden según las reglas configuradas. El engine integra esta lógica con las mediciones reales, el estado de las habitaciones y la comunicación con los switches.

### Responsabilidades principales

- **Evaluación de temperaturas** permite determinar las acciones de control a partir de las temperaturas actuales y esperadas.

- **Reglas de negocio** concentra las decisiones del sistema sin depender directamente de MQTT, MongoDB o los controladores REST.

- **Evaluación de condiciones de operación** permite modelar las reglas que correspondan al funcionamiento del sistema, como las relacionadas con las tarifas, horarios rushHour, etc.

Esta separación permite mantener el core enfocado en las decisiones del sistema y el engine encargado de integrarlas con el entorno real.

## 7. MÓDULO: SWITCHES-API
**Contiene la API REST encargada de consultar y modificar el estado de los switches del sistema.**\
Está desarrollada utilizando Spring Boot. 
El módulo es utilizado por el controlador automático de temperatura para activar o desactivar el switch correspondiente según la temperatura actual de una habitación.
- **Endpoints principales**
  - GET /switches/{id}: obtiene el estado actual de un switch.
  - POST /switches/{id}: actualiza el estado del switch, permitiendo encenderlo o apagarlo.
## 8. SWAGGER/OPENAPI
Swagger permite visualizar y probar los endpoints disponibles de cada API desde una interfaz web, mostrando los métodos HTTP, parámetros, cuerpos de las solicitudes y respuestas esperadas.

Es una alternativa más sencilla y fácil de visualizar que los ejemplos curl.
## 9. TEST UNITARIOS

Se crearon test para probar tramos del programa y confirmar el funcionamiento de envío y recibimiento de mensajes y datos.

**controlador**

- **controlador/api**
  - ApiKeyTest
  - EngineContractTest
  - HabitacionControllerTest
  - HabitacionServiceTest
  - InventarioServiceTest
  - RoomServiceTest

- **controlador/engine**
  - ControllerTempAutoTest
  - EstadoSitioServiceTest
  - MongoTemperatureTest
  - MqttSubscriberTest
  - SwitchClientTest

- **controlador/integration**
  - IntegrationServiceTest
  - MqttSubscriberTest

**core**
- CoreTest
- PuntaTest

**publisher**
- PublisherUnitTest

**switches-api**
- SwitchControllerTest

## 10. MONGO DB

Se creo una base de datos llamada **Sensores**, dentro de esta se encuentran dos colecciones:

- inventory: contiene sitios y sus datos. Cada site contiene una lista de habitaciones con sus respectivos datos.
- Mediciones: recibe los datos del mensaje: id, temperatura en Celsius, temperatura en Fahrenheit y marca de tiempo Epoch en milisegundos.

## 11. JIRA

Acá se encuentra la organización y seguimiento del proyecto.

[Link a Jira](https://walterlarrea.atlassian.net/jira/software/projects/SCRUM/boards/1?filter=&groupBy=none&atlOrigin=eyJpIjoiYjAzY2VmYzE5OTgzNDFiNjgyNDEzM2Y2OTA2ODI2NjgiLCJwIjoiaiJ9)
