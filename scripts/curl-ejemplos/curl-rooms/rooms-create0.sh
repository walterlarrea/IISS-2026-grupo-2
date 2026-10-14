# Crea la habitación nº0.

curl -X POST http://localhost:8080/habitaciones \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-rooms-key" \
  -d '{
    "nombre": "Living Room",
    "temperaturaEsperada": 24.0,
    "idTermostato": "0",
    "uriSwitch": "http://switches-api:8090/switches/0"
  }'