#!/bin/bash
# Script para crear datos de prueba (validos e invalidos) a traves de la API de habitaciones
# y verificar el endpoint GET /habitaciones/validar

API_URL="${ROOMS_API_URL:-http://localhost:8080}"
API_KEY="${ROOMS_API_KEY:-dev-rooms-key}"

echo "=========================================================="
echo "Creando datos de prueba en la API: $API_URL"
echo "=========================================================="

crear_habitacion() {
  local descripcion="$1"
  local payload="$2"

  echo ""
  echo "--> Creando: $descripcion"
  curl -s -X POST "$API_URL/habitaciones" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $API_KEY" \
    -d "$payload"
  echo ""
}

# 1. Habitacion totalmente valida 1
crear_habitacion "Habitacion Valida 1" '{
  "nombre": "Sala de Estar (Valida 1)",
  "temperaturaEsperada": 22.0,
  "idTermostato": "termo-valido-01",
  "uriSwitch": "http://switches-api:8090/switches/01"
}'

# 2. Habitacion totalmente valida 2
crear_habitacion "Habitacion Valida 2" '{
  "nombre": "Cocina (Valida 2)",
  "temperaturaEsperada": 20.5,
  "idTermostato": "termo-valido-02",
  "uriSwitch": "http://switches-api:8090/switches/02"
}'

# 3. Caso Invalido: idTermostato vacio / en blanco
crear_habitacion "Caso Invalido: idTermostato vacio" '{
  "nombre": "Habitacion idTermostato Vacio",
  "temperaturaEsperada": 19.0,
  "idTermostato": "   ",
  "uriSwitch": "http://switches-api:8090/switches/03"
}'

# 4. Caso Invalido: uriSwitch malformada (no es URI valida)
crear_habitacion "Caso Invalido: uriSwitch no valida" '{
  "nombre": "Habitacion uriSwitch Invalida",
  "temperaturaEsperada": 21.0,
  "idTermostato": "termo-valido-04",
  "uriSwitch": "no-es-una-uri-valida"
}'

# 5. Caso Duplicado: dos habitaciones con mismo idTermostato
crear_habitacion "Caso Duplicado: Termostato Duplicado (Habitacion A)" '{
  "nombre": "Dormitorio A (Termostato Duplicado)",
  "temperaturaEsperada": 23.0,
  "idTermostato": "termo-duplicado-99",
  "uriSwitch": "http://switches-api:8090/switches/98"
}'

crear_habitacion "Caso Duplicado: Termostato Duplicado (Habitacion B)" '{
  "nombre": "Dormitorio B (Termostato Duplicado)",
  "temperaturaEsperada": 23.5,
  "idTermostato": "termo-duplicado-99",
  "uriSwitch": "http://switches-api:8090/switches/99"
}'

# 6. Caso Duplicado: dos habitaciones con mismo uriSwitch
crear_habitacion "Caso Duplicado: Switch Duplicado (Habitacion C)" '{
  "nombre": "Pasillo C (Switch Duplicado)",
  "temperaturaEsperada": 18.0,
  "idTermostato": "termo-switch-dup-01",
  "uriSwitch": "http://switches-api:8090/switches/switch-compartido"
}'

crear_habitacion "Caso Duplicado: Switch Duplicado (Habitacion D)" '{
  "nombre": "Pasillo D (Switch Duplicado)",
  "temperaturaEsperada": 18.5,
  "idTermostato": "termo-switch-dup-02",
  "uriSwitch": "http://switches-api:8090/switches/switch-compartido"
}'

echo ""
echo "=========================================================="
echo "Consultando endpoint de validacion: GET $API_URL/habitaciones/validar"
echo "=========================================================="
echo ""
curl -s -X GET "$API_URL/habitaciones/validar" \
  -H "X-API-Key: $API_KEY"

echo ""
echo ""
echo "Prueba finalizada."
