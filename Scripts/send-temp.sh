#!/bin/bash
curl -s -d '{"temperatura": 24.1, "humedad": 50}' mqtt://localhost:1883/home/sensors/temperature