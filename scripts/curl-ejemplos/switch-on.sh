# Encender el switch.

curl -X POST http://localhost:8090/switches \
  -H "Content-Type: application/json" \
  -d '{
    "id": "switch-0",
    "encendido": true
  }'