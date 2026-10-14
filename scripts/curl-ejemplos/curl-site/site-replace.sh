# Reemplazar el inventario. Checkear bien los campos y direcciones.

curl -X PUT http://localhost:8080/sitio \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "schemaVersion": "2.0",
        "sitio": {
          "_id": "casa-ejemplo",
          "nombre": "Casa de ejemplo",
          "potenciaContratadaKW": 4.7,
          "tarifa": {
            "punta": {
              "desde": "17:00",
              "hasta": "23:00",
              "dias": "HABILES"
            }
          }
        },
        "habitaciones": [
          {
            "_id": "room1",
            "nombre": "Living",
            "temperaturaEsperada": 23.5,
            "potenciaKW": 3.1,
            "idTermostato": "ht-room1",
            "topicTermostato": "ht-room1/status/temperature:0",
            "idSwitch": "sw-room1",
            "urlSwitch": "http://host.docker.internal:9090"
          },
          {
            "_id": "room2",
            "nombre": "Dormitorio",
            "temperaturaEsperada": 20,
            "potenciaKW": 0.8,
            "idTermostato": "ht-room2",
            "topicTermostato": "ht-room2/status/temperature:0",
            "idSwitch": "sw-room2",
            "urlSwitch": "http://host.docker.internal:9090"
          }
        ]
      }'