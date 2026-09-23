# PROYECTO DE SENSORES VERSION 3.

## 1. ESTRUCTURA

**Estructura del proyecto**

- **publisher/**, **subscriber/**, **rooms-api/** y **switches-api/** : módulos Maven del proyecto.
- **Módulos**
  - **MqttSubscriber.java**  programa escrito en Java para conectar al broker MQTT y recibir los mensajes.
  - **MongoTemperatureWriter**  programa escrito en Java para guardar mensajes que le llegan al subscriptor en base de datos MongoDB.
  - **ControllerTempAuto**  controlador de temperatura automático, se inicia, detiene y controla la temperatura esperada de la habitación.
  - **EventPublisher.java**  programa escrito en Java para enviar mensajes hacia el subscriptor que esté escuchando.
  - **EventGenerator.java**  crea mensajes de los sensores constantemente para enviar al Subscriber.
  - **ApiKeyRoom**  intercepta las solicitudes HTTP antes de que lleguen al controlador y verifica que contengan una clave válida.
  - **RoomController.java**  se encarga de exponer la API REST para la gestión de las habitaciones. Recibe las solicitudes HTTP relacionadas con las habitaciones.
    - GET /habitaciones: obtiene todas las habitaciones.
    - GET /habitaciones/{id}: obtiene una habitación por su id.
    - GET /habitaciones/termostato/{idTermostato}: obtiene la habitación asociada a un termostato.
    - GET /habitaciones/validar: valida las habitaciones registradas.
    - POST /habitaciones: crea una nueva habitación.
    - PUT /habitaciones/{id}: modifica una habitación existente.
    - PATCH /habitaciones/{id}: modifica parcialmente una habitación.
    - DELETE /habitaciones/{id}: elimina una habitación.
  - **SwitchController** se encarga de exponer la API REST para la gestión de los switches. Recibe las solicitudes HTTP relacionadas con el estado de los switches.
- **pom.xml**  agregador Maven raíz que contiene las dependencias y detalles de todos los módulos.

## 2. SCRIPTS y CONF

1. docker --help, en consola para ver sus respectivos comandos.
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
- **Curl-ejemplos**
  - **ctrl-start.sh** iniciar el controlador automático de temperatura.
  - **ctrl-status.sh** consultar si está activo el controlador automático de temperatura.
  - **ctrl-stop.sh** detener el controlador automático de temperatura.
  - **rooms-create0.sh** crea la habitación nº0.
  - **rooms-create1.sh** crea la habitación nº1.
  - **rooms-delete.sh** elimina una habitación por su ID.
  - **rooms-find.sh** busca la habitación por ID de termostato.
  - **rooms-list.sh** lista todas las habitaciones.
  - **rooms-patch.sh** modificar la temperatura esperada de la habitación por su ID.
  - **rooms-validar.sh** validar todas las habitaciones de la coleccion (detecta inválidas o duplicadas).
  - **switch-off.sh** apaga el switch.
  - **switch-on.sh** enciende el switch.
  - **switch-status-sw0.sh** consulta el estado actual del switch 0.

## 3. EJECUCIÓN

**Como ejecutar el programa en Java**

1. Abrir el proyecto desde su IDE.
2. Asegurarse de que este en JDK-25.
3. Ejecutar el programa desde el Main.java
4. El programa se conectará al broker MQTT.
5. Una vez conectado se suscribirá al tópico : "#".
   - El '#' es para que reciba todos los mensajes que llegan a los tópicos del broker.
6. Cualquier mensaje recibido lo mostrará en la consola.

**Datos de conexión necesarios**

**broker**: tcp://localhost:1883 **(esto en caso de que sea una conexión únicamente local, sino: tcp://mosquitto:1883)**

**clientId**: JavaSubscriber **(local, si es dev: JavaSubscriberDev)**

**topic**: "#"

## 4. MÓDULO: PUBLISHER

**Contiene un generador de eventos aleatorios con diferentes tópicos para sus respectivas habitaciones.**\
Este manda mensajes al broker cada 3 segundos, con una id del dispositivo que lo envía, su temperatura y la fecha de envío como Epoch en milisegundos.

## 5. MÓDULO: SUBSCRIBER

**Contiene un vínculo a una base de datos (MongoDB), donde guarda los mensajes que recibe de cada sensor al que está subscrito.**\
Escucha los mensajes que le envía el Publisher.

## 6. MÓDULO: ROOMS-API

## 7. MÓDULO: SWITCHES-API

## 8. TEST UNITARIOS

Se crearon test para probar tramos del programa y confirmar el funcionamiento de envío y recibimiento de mensajes y datos.

**publisher**
- PublisherUnitTest

**subscriber**
- MqttSubscriberTest
- MongoTemperatureTest
- ControllerTempTest

**rooms-api**
- ApiKeyTest
- RoomControllerTest
- RoomServiceTest

**switches-api**
- SwitchControllerTest

## 9. MONGO DB

Se creo una base de datos llamada **Sensores**, dentro de esta se encuentran dos colecciones:

- Habitaciones: muestra la id, nombre de la habitación, la temperatura esperada, el id del termostato y el uri del switch.
- Mediciones: recibe los datos del mensaje: id, temperatura en Celsius, temperatura en Fahrenheit y marca de tiempo Epoch en milisegundos.

## 10. JIRA

Acá se encuentra la organización y seguimiento del proyecto.

[Link a Jira](https://walterlarrea.atlassian.net/jira/software/projects/SCRUM/boards/1?filter=&groupBy=none&atlOrigin=eyJpIjoiYjAzY2VmYzE5OTgzNDFiNjgyNDEzM2Y2OTA2ODI2NjgiLCJwIjoiaiJ9)
