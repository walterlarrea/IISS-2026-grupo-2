# Arquitectura 4+1 — EcoWarm Iteración 4

**Proyecto:** EcoWarm — Sistema de control de temperature domótico  
**Grupo:** Grupo 2  
**Iteración:** 4

---

## 1. Vista Lógica

El sistema se divide en dos módulos conceptuales bien diferenciados:

### Core (`com.iiss.sensores.grupo_2`)

Lógica de decisión pura, sin dependencias de frameworks ni red. Recibe un snapshot del estado del site y devuelve una lista de commands a ejecutar.

Interfaz principal:
```java,
ICore.calcularComandos(ZonedDateTime fechaHora, DataSitio dataSite): RespuestaCore
```

**Modelos de entrada (`modelos/input`):**

| Clase | Responsabilidad |
|---|---|
| `DataSitio` | Agrupa el site y la lista de habitaciones |
| `Sitio` | Nombre, potencia contratada (kW) y tarifa |
| `Habitacion` | Temperatura actual/esperada, potencia, id del termostato y URL del switch |
| `Tarifa` / `Punta` | Franja horaria rushHour (ej. 17:00–23:00, días hábiles) |

**Modelos de salida (`modelos/output`):**

| Clase | Responsabilidad |
|---|---|
| `RespuestaCore` | Lista de commands calculados |
| `ComandoSwitch` | Switch a accionar con estado destino (ENCENDIDO / APAGADO) |

**Reglas implementadas en `Core`:**
1. Si el horario es **rushHour**, apaga todos los switches activos.
2. Apaga las habitaciones que ya alcanzaron su temperature objetivo (liberando potencia).
3. Enciende las habitaciones con mayor déficit primero, respetando el límite de potencia contratada del site.

### Engine (`com.iiss.sensores.grupo_2.controlador`)

Capa de infraestructura Spring Boot que conecta el Core con el mundo exterior. Se organiza en subcapas:

| Subcapa | Paquete | Responsabilidad |
|---|---|---|
| API REST | `.api` | Exposición HTTP: CRUD de habitaciones, inventory del site, control del engine |
| MQTT | `.engine.mqtt` | Suscripción al broker y despacho a `ControllerTempAuto` |
| Switch Client | `.engine.client` | Envío de commands ON/OFF a los switches via HTTP |
| Writer | `.engine.writer` | Persistencia de mediciones de temperature en MongoDB |
| Integration | `.integration` | Registro en el simulador y configuración automática del broker MQTT |
| Shared | `.shared` | Interfaces transversales (`RoomProvider`, `TemperatureEngineService`) |

El `ControllerTempAuto` implementa `TemperatureEngineService` y actúa como puente: recibe la temperature por termostato desde el `MqttSubscriber`, consulta la habitación asociada vía `RoomProvider` y decide encender o apagar el switch.

> **Nota sobre el Core en la iteración actual:** `ControllerTempAuto.controlarHab()` aplica directamente la lógica de comparación temperature actual vs. esperada. La integración completa con `ICore.calcularComandos()` (que evalúa potencia contratada, tarifa rushHour y priorización por déficit) queda como tarea pendiente para conectar ambas capas.

---

## 2. Vista de Procesos

Flujo principal al recibir un evento de temperature por MQTT:

```
[Simulador / Sensor]       [Engine - MqttSubscriber]     [Core]        [Switch REST]
        │                           │                        │                │
        │── pub. temperature ──────>│                        │                │
        │                           │── saveTemperature() ──────> MongoDB     │
        │                           │                        │                │
        │                           │── controlarHab(tC, id) │                │
        │                           │     │                  │                │
        │                           │     │ obtenerPorTermostato(id)          │
        │                           │     │<── RoomInfo ─────│                │
        │                           │     │                  │                │
        │                           │     si tC < tEsperada  │                │
        │                           │──────────────────────────── POST /ON ──>│
        │                           │     si tC > tEsperada  │                │
        │                           │──────────────────────────── POST /OFF ─>│
```

**Flujo de inicio (integración con simulador):**

Al arrancar, `SimulatorIntegrationService` se registra en el simulador vía REST, obtiene el inventory del site (habitaciones, termostatos, switches), lo persiste, y conecta el `MqttSubscriber` al broker indicado por el simulador. Si el simulador no está disponible, reintenta cada 5 segundos.

---

## 3. Vista de Desarrollo

Estructura de módulos Maven en el repositorio:

```
/
├── core/              # Java puro — lógica de negocio, TDD
│   └── src/main/java/com/iiss/sensores/grupo_2/
│       ├── interfaces/    ICore, IComando
│       ├── modelos/input/ DataSitio, Sitio, Habitacion, Tarifa, Punta
│       ├── modelos/output/RespuestaCore, Comando, ComandoSwitch
│       └── enums/         EstadoSwitch, TipoAccion, DiasPunta
│
├── common/            # DTOs compartidos entre módulos (MessageObject, etc.)
│
├── controlador/       # Spring Boot — API REST + Engine MQTT
│   └── src/main/java/com/iiss/sensores/grupo_2/controlador/
│       ├── api/           model, repository, service, controller
│       ├── engine/        mqtt, service (ControllerTempAuto), client, writer
│       ├── integration/   SimulatorIntegrationService
│       └── shared/        RoomProvider, TemperatureEngineService, RoomInfo
│
├── publisher/         # Simulador local de publicación de eventos MQTT
├── switches-api/      # Stub REST de switches (para desarrollo sin simulador)
│
└── docker/
    └── docker-compose.yml
```

**Dependencias entre módulos:**
- `controlador` depende de `core` y `common`.
- `core` es standalone (sin dependencias de infraestructura).
- `publisher` y `switches-api` son auxiliares independientes.

**Cobertura de tests:**
- `core`: `CoreTest.java` — prueba todas las reglas (horario rushHour, priorización por déficit, límite de potencia).
- `controlador`: tests de API Key, controllers, services, MQTT subscriber, Switch client e integración con simulador.

---

## 4. Vista Física

Despliegue con Docker Compose (archivo `docker/docker-compose.yml`):

```
┌────────────────────────────────────────────────────────────┐
│                     Docker Compose                         │
│                                                            │
│  ┌─────────────┐   MQTT    ┌───────────────┐              │
│  │  publisher  │─────────> │  mosquitto    │ :1883        │
│  │ (simulador  │           │  (broker)     │              │
│  │  de eventos)│           └──────┬────────┘              │
│  └─────────────┘                  │ MQTT                  │
│                                   ▼                        │
│  ┌──────────────────────────────────────────────┐         │
│  │           controlador  :8080                 │         │
│  │   API REST + Engine MQTT + Core              │         │
│  │                          │                   │         │
│  │                          │ MongoDB           │         │
│  └──────────────────────────┼───────────────────┘         │
│                             ▼                              │
│                    ┌──────────────┐                        │
│                    │   mongodb    │ :27017                 │
│                    └──────────────┘                        │
│                                                            │
│  ┌─────────────┐                                           │
│  │ switches-api│ :8090  (stub REST de switches)            │
│  └─────────────┘                                           │
└────────────────────────────────────────────────────────────┘

         ┌──────────────────────┐
         │ Simulador (cátedra)  │  (externo, opcional)
         │  MQTT + REST         │
         └──────────────────────┘
```

En modo de integración con el simulador de la cátedra, `mosquitto` y `switches-api` son reemplazados por los servicios provistos por el simulador; el contenedor `controlador` se registra automáticamente al iniciar.

---

## 5. Escenario (+1)

**Control automático de temperature por habitación**

1. El simulador publica una lectura de temperature vía MQTT (ej. `{"id": "ht-sim-room1", "tC": 18.5}`).
2. `MqttSubscriber` recibe el mensaje, persiste la medición en MongoDB y llama a `ControllerTempAuto.controlarHab()`.
3. El engine consulta qué habitación corresponde al termostato y compara la temperature recibida con la esperada.
4. Si `tC < tEsperada`: envía `POST /ON` al switch de esa habitación.  
   Si `tC ≥ tEsperada`: envía `POST /OFF`.
5. El switch (del simulador o del stub) actualiza su estado.

El sistema responde evento a evento, sin ciclos de polling. Cada mensaje MQTT dispara una decisión independiente en el Engine, delegando al Core la lógica de negocio compleja (tarifa rushHour, potencia máxima) cuando está integrado.
