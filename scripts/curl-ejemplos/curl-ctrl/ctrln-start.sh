# Iniciar el controlador. Modificar el factor tiempo a gusto.

curl -X POST http://localhost:8080/control/comandos \
  -H "Content-Type: application/json" \
  -d '{
    "accion": "INICIAR",
    "fechaHora": "2026-10-14T13:00:00",
    "factorTiempo": 10.0
  }'