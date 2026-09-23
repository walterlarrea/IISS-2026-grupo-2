# Validar todas las habitaciones de la coleccion (detecta invalidas o duplicadas).

curl -X GET http://localhost:8080/habitaciones/validar \
  -H "X-API-Key: dev-rooms-key"
