## Resumen del módulo Switches API

El módulo `switches-api` expone una API REST para consultar y cambiar el estado de switches. Los estados se guardan en memoria y no se persisten en una base de datos.

### ¿Qué hace?

- Consulta el estado actual de un switch mediante su ID.
- Enciende o apaga un switch mediante una petición `POST`.
- Crea automáticamente los switches que todavía no existen.
- Inicia cada switch nuevo en estado apagado (`false`).
- Valida que las peticiones `POST` tengan un ID y un booleano válidos.
- Protege los endpoints mediante una API key.

### ¿Cómo funciona?

- El servicio se inicia con Spring Boot.
- Guarda los estados en un `ConcurrentHashMap`.
- Usa `GET /switches/{id}` para consultar un switch.
- Usa `POST /switches` para actualizar su estado.
- En cada petición:
  1. valida el header `X-API-KEY`,
  2. crea el switch si no existe,
  3. consulta o actualiza el estado,
  4. devuelve una respuesta JSON.

### Consultar un switch

Petición:

```http
GET /switches/switch-1
X-API-KEY: development-key
```

"encendido": false
}

````
```http
POST /switches
Content-Type: application/json
X-API-KEY: development-key
````

"id": "switch-1",
"encendido": true
}

1. valida la API key,
2. crea el switch si no existe,
3. consulta o actualiza el estado,
4. devuelve una respuesta JSON.

- La API key se envía en el header `X-API-KEY`.
- Devuelve `401 Unauthorized` si falta la API key.
- Devuelve `403 Forbidden` si la API key es incorrecta.
- Devuelve `400 Bad Request` si el body tiene un formato o tipos inválidos.
- Devuelve `200 OK` cuando la petición se procesa correctamente.

### Configuración

- Requiere Java 25.
- En el perfil `development` utiliza el puerto `8090`.
- En el perfil `local` utiliza el puerto `8091`.
  Ejemplo para development:

````text
SPRING_PROFILES_ACTIVE=development
SWITCHES_API_PORT=8090
```bash
docker compose -f docker/docker-compose.yml up --build switches-api
````

Se puede cambiar el puerto y la API key mediante las variables de entorno correspondientes.

### En pocas palabras

Es una API REST sencilla para simular switches IoT: mantiene sus estados en memoria, permite consultarlos y modificarlos, y utiliza una API key para controlar el acceso.
