#!/bin/bash
mosquitto_pub -h localhost -p 1883 -t "home/sensors/temperature" -m "temperatura: 23.5, humedad: 55"