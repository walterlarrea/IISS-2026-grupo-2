# Crea la habitación nº1.

curl -X POST http://localhost:8080/habitaciones \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-rooms-key" \
  -d '{
    "nombre": "Dormitorio Principal",
    "temperaturaEsperada": 22.0,
    "idTermostato": "1",
    "uriSwitch": "http://switches-api:8090/switches/1"
  }'