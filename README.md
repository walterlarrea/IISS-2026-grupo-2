# PROYECTO DE SENSORES VERSION 2.

## 1. ESTRUCTURA

**Estructura del proyecto**

- **scr/** : código fuente del proyecto.
- **Módulos**
	- **MqttSubscriber.java** : programa escrito en Java para conectar al broker MQTT y recibir los mensajes.
		- **MongoTemperatureWriter** : programa escrito en Java para guardar mensajes que le llegan al subscriptor en base de datos MongoDB.
	- **EventPublisher.java** : programa escrito en Java para enviar mensajes hacia el subscriptor que esté escuchando.
		- **EventGenerator.java** : crea mensajes de los sensores constantemente para enviar al Subscriber.
- **pom.xml** : contiene las dependencias y detalles del proyecto, contiene tres: para Publisher, para Subscriber y para el padre que abarca a ambos.


## 2. SCRIPTS y CONF

1) docker --help, en consola para ver sus respectivos comandos.
2) Para ejecutar los scripts .sh en la consola. Ej: **./up.sh** o **bash up.sh** o **sh up.sh**
3) Para ejecutar el script build. sh, se puede usar: build.sh local (para .env.local) o build.sh dev (para .env.dev). Si no se escribe nada posterior al .sh, tomará a dev por defecto.

 **#!/bin/bash --> se tiene que ejecutar usando Bash**

- **up.sh** sirve para levantar Docker compose y crear la imagen antes de iniciar los contenedores.
- **down.sh** sirve para detener y eliminar los contenedores.
- **stop.sh** sirve para detener pero sin eliminar los contenedores.
- **build.sh** sirve para crear la imagen en Docker.
- **receive-temp.sh** se suscribe a un tópico MQTT y muestra en la consola todos los mensajes que lleguen de dicho tópico.
- **send-temp.sh** publica los mensajes en un tópico MQTT y estos le llegan a quien esté suscripto.
## 3. EJECUCIÓN

**Como ejecutar el programa en Java**

1) Abrir el proyecto desde su IDE.
2) Asegurarse de que este en JDK-25.
3) Ejecutar el programa desde el Main.java
4) El programa se conectará al broker MQTT.
5) Una vez conectado se suscribirá al tópico : "#". 
	- El '#' es para que reciba todos los mensajes que llegan a los tópicos del broker.
6) Cualquier mensaje recibido lo mostrará en la consola.

**Datos de conexión necesarios**

**broker**: tcp://localhost:1883 **(esto en caso de que sea una conexión únicamente local, sino: tcp://mosquitto:1883)**

**clientId**: JavaSubscriber **(local, si es dev: JavaSubscriberDev)**

**topic**: "#"

## 4. MÓDULO: PUBLISHER

**Contiene un generador de eventos aleatorios con diferentes tópicos para sus respectivas habitaciones.**\
Este manda mensajes al broker cada 3 segundos, con una id del dispositivo que lo envía, su temperatura y la fecha de envío en medida Epoch.

## 5. MÓDULO: SUBSCRIBER
**Contiene un vínculo a una base de datos (MongoDB), donde guarda los mensajes que recibe de cada sensor al que está subscrito.**\
Escucha los mensajes que le envía el Publisher.
## 6. TEST UNITARIOS
Se crearon test para probar tramos del programa y confirmar el funcionamiento de envío y recibimiento de mensajes.
- MqttSubscriberTest
- MqttSubscriberConfigTest
- MongoTemperatureTest
- PublisherUnitTest

## 7. MONGO DB
Se creo una base de datos llamada **Sensores**, dentro de esta se encuentran dos colecciones:
- Habitaciones: muestra la id del sensor y el nombre de la habitación.
- Mediciones: recibe los datos del mensaje: id, temperatura en Celsius, temperatura en Fahrenheit y medida de tiempo en Epoch.
## 8. JIRA

Acá se encuentra la organización y seguimiento del proyecto.

[Link a Jira](https://walterlarrea.atlassian.net/jira/software/projects/SCRUM/boards/1?filter=&groupBy=none&atlOrigin=eyJpIjoiYjAzY2VmYzE5OTgzNDFiNjgyNDEzM2Y2OTA2ODI2NjgiLCJwIjoiaiJ9)

