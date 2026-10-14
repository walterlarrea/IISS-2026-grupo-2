# Modificar la temperatura esperada de la habitación por su ID.

curl -X PATCH http://localhost:8080/habitaciones/ID_DE_LA_HABITACION \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-rooms-key" \
  -d '{
    "temperaturaEsperada": 25.5
  }'
