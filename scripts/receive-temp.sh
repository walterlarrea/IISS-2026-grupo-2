#!/bin/bash
# Se suscribe al topic MQTT de temperatura y muestra los mensajes recibidos.
curl -N -o - mqtt://localhost:1883/home/sensors/temperature