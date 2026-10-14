# Detener el controlador.

curl -X POST http://localhost:8080/control/comandos \
  -H "Content-Type: application/json" \
  -d '{
    "accion": "DETENER"
  }'