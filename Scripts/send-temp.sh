#!/bin/bash
mosquitto_pub -h localhost -p 1884 -t "home/sensors/temperature" -m "temperatura: 23.5, humedad: 55"