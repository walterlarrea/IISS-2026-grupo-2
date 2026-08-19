# PROYECTO DE SENSORES VERSION 1.

## 1. ESTRUCTURA

**Estructura del proyecto**

- **scr/** : código fuente del proyecto.
- **MqttSubscriber.java** : programa escrito en Java para conectar al broker MQTT y recibir los mensajes.
- **pom.xml** : contiene las dependencias y detalles del proyecto.


## 2. SCRIPTS y CONF

1) docker --help, en consola para ver sus respectivos comandos.
2) Para ejecutar los scripts .sh en la consola. Ej: **./up.sh** o **bash up.sh** o **sh up.sh**

 **#!/bin/bash --> se tiene que ejecutar usando Bash**

- **up.sh** sirve para levantar Docker compose y crear la imagen antes de iniciar los contenedores.
- **down.sh** sirve para detener y eliminar los contenedores.
- **stop.sh** sirve para detener pero sin eliminar los contenedores.
- **receive-temp.sh** se suscribe a un tópico MQTT y muestra en la consola todos los mensajes que lleguen de dicho tópico.
- **send-temp.sh** publica los mensajes en un tópico MQTT y estos le llegan a quien esté suscripto.

## 3. SUSCRIPTOR JAVA

**Cómo ejecutar el programa en Java**

1) Abrir el proyecto desde su IDE.
2) Asegurarse de que este en JDK-25.
3) Ejecutar el programa desde el Main.java
4) El programa se conectará al broker MQTT.
5) Una vez conectado se suscribirá al tópico : "home/sensors/temperature".
6) Cualquier mensaje recibido lo mostrará en la consola.

**Datos de conexión necesarios**

broker: tcp://localhost:1883 (esto en caso de que sea una conexión únicamente local).

clientId: JavaSubscriber

topic: "home/sensors/temperature"

## 4. CLIENTE MQTT

**Como inicializar Explorer y comprobar el funcionamiento del sistema**

1) Abrir MQTT Explorer y crear una conexión donde: 
	Host: localhost
	Port : 1883
	Protocol: mqtt
**al permitir todo usuario que este en la misma red, no es necesario indicar usuario y contraseña.

2) Publicar en el tópico "home/sensors/temperature" un mensaje. Ej: 16ºC.
3) Este mensaje debería poder verse en la consola del programa Java. Ej: 16ºC.

## 5. JIRA

Acá se encuentra la organización y seguimiento del proyecto.

[Link a Jira](https://walterlarrea.atlassian.net/jira/software/projects/SCRUM/boards/1?filter=&groupBy=none&atlOrigin=eyJpIjoiYjAzY2VmYzE5OTgzNDFiNjgyNDEzM2Y2OTA2ODI2NjgiLCJwIjoiaiJ9)

