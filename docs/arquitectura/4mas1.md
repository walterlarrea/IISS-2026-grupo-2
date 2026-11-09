# Arquitectura 4+1 — EcoWarm (Iteración 5)

**Proyecto:** EcoWarm — Sistema de control de temperatura domótico  
**Grupo:** Grupo 2  
**Iteración:** 5 (Calidad, tiempo acelerado, resiliencia y cobertura)

---

## 1. Introducción y Contexto

El sistema EcoWarm tiene como propósito gestionar de forma automatizada y eficiente la climatización de un sitio (hogar
inteligente). Para la **Iteración 5**, la arquitectura evoluciona garantizando características clave de calidad y
resiliencia:

- Integración real y bidireccional entre el **Core de decisión** y el **Engine de control**.
- Soporte para **tiempo acelerado y virtualizado** (simulaciones con factor de aceleración $X$).
- Resiliencia y tolerancia a fallos: reconexión automática ante caídas de MQTT broker, indisponibilidad de la API REST
  de switches y control de límites de potencia contratada.
- Registro y actualización dinámica del inventario del sitio ante cambios durante la ejecución.

---

## 2. Vista Lógica

La arquitectura desacopla estrictamente la lógica de negocio pura de la infraestructura y comunicaciones.

```mermaid
graph TD
    subgraph Core ["Módulo Core (Java Puro - Dominio)"]
        ICore["ICore"]
        CoreImpl["Core"]
        DataSite["DataSite (Snapshot)"]
        CoreAnswer["CoreAnswer (Comandos)"]
        RushHour["RushHour / Fee"]
        CoreImpl -.->|implements| ICore
        CoreImpl --> DataSite
        CoreImpl --> RushHour
        CoreImpl --> CoreAnswer
    end

    subgraph Engine ["Módulo Controlador / Engine (Spring Boot)"]
        EngineCtrl["EngineController (REST)"]
        EngineSvc["TemperatureEngineService (interface)"]
        SiteCtrl["SiteController / RoomController"]
        IntService["IntegrationService"]
        InvSvc["InventoryService"]
        MqttSub["MqttSubscriber (Paho MQTT)"]
        MqttSync["MqttSubscriptionService"]
        Reeval["ReevaluationControlService"]
        TempAuto["TemperatureControlEngine"]
        SiteState["SiteStateService (Enriquecedor de Estado)"]
        SwitchCli["SwitchClient (REST Client)"]
        MongoRepo["TemperatureReadingRepository"]
        EngineCtrl --> EngineSvc
        EngineSvc --> TempAuto
        SiteCtrl --> InvSvc
        InvSvc --> SiteState
        IntService --> MqttSync
        MqttSync --> MqttSub
        MqttSub --> MongoRepo
        MqttSub --> TempAuto
        Reeval --> TempAuto
        TempAuto --> SiteState
        TempAuto --> ICore
        TempAuto --> SwitchCli
    end

    subgraph Infra ["Infraestructura y Servicios Externos"]
        Simulator["Simulador de Cátedra (REST)"]
        Broker["Broker MQTT (Mosquitto / Simulador)"]
        Switches["Switches REST (Simulador / Stub)"]
        MongoDB[("MongoDB")]
    end

    IntService -.->|POST /controladores| Simulator
    Broker -.->|Eventos temp| MqttSub
    SwitchCli -.->|POST ON/OFF| Switches
    MongoRepo -.-> MongoDB
```

### Componentes Principales:

1. **Core (`com.iiss.sensores.grupo_2`):**
    - **Independiente de infraestructura:** No contiene librerías web, bases de datos ni frameworks.
    - **Contrato principal:** `ICore.calcularComandos(ZonedDateTime fechaHora, DataSite dataSite) : CoreAnswer`.
    - **Reglas de negocio:**
        - *Tarifa Punta (`RushHour`):* Si el horario evaluado (en UTC-3) cae en franja punta de día hábil, emite orden
          de `APAGADO` para todos los switches activos.
        - *Liberación de potencia:* Apaga switches en habitaciones que alcanzaron o superaron la temperatura objetivo.
        - *Priorización por déficit:* Ordena habitaciones por mayor diferencia $(T_{esperada} - T_{actual})$ y enciende
          switches hasta el límite de `potenciaContratadaKW`.

2. **Engine / Controlador (`com.iiss.sensores.grupo_2.controlador`):**
    - **`TemperatureControlEngine`:** Implementa `TemperatureEngineService` y orquesta el ciclo de control. Administra el
      estado (`INICIAR` / `DETENER`), el factor de aceleración de tiempo y la conversión a zona horaria local
      (`America/Montevideo`). Invoca a `ICore` y despacha los comandos resultantes vía `SwitchClient`.
    - **`SiteStateService`:** Mantiene el estado en memoria, enriqueciendo las habitaciones con las últimas lecturas
      térmicas obtenidas desde MongoDB o memoria, y realiza hidratación diferida de habitaciones pendientes.
    - **`InventoryService`:** Guarda el inventario activo y notifica cambios de configuración mediante `UpdatedInventoryEvent`.
      `SiteStateService.loadSiteState(...)` reacciona a esos cambios y repuebla el estado de runtime apropiadamente.
    - **`IntegrationService` y `MqttSubscriptionService`:** `IntegrationService` registra automáticamente el controlador en
      el simulador al iniciar (`ApplicationReadyEvent`) y, a partir del inventario activo, `MqttSubscriptionService`
      configura los topics MQTT a escuchar y los re-suscribe cuando cambia el sitio.
    - **`MqttSubscriber`:** Cliente Eclipse Paho con reconexión automática (`AutomaticReconnect = true`) y
      sincronización ante desconexiones.
    - **`ReevaluationControlService`:** Tarea programada (`@Scheduled`) que reevalúa periódicamente el estado aunque no
      ingresen nuevas lecturas de sensores.

---

## 3. Vista de Procesos

La vista de procesos describe la concurrencia, sincronización e interacción ante eventos del simulador.

### Secuencia: Recepción de Lectura MQTT y Ciclo de Control

```mermaid
sequenceDiagram
    autonumber
    actor Sensor as Simulador / Sensor
    participant Broker as MQTT Broker
    participant MqttSub as MqttSubscriber
    participant Repo as Mongo Temp Repo
    participant Engine as TemperatureControlEngine
    participant State as SiteStateService
    participant Core as Core (ICore)
    participant Switch as Switch REST API
    Sensor ->> Broker: Publica temperatura (tC, idTermostato)
    Broker ->> MqttSub: messageArrived(topic, payload)
    MqttSub ->> Repo: saveTemperature(lectura)
    MqttSub ->> Engine: processTemperature(lectura)
    activate Engine
    Engine ->> State: getSiteState()
    State -->> Engine: DataSite (estado actual habitaciones)
    Note over Engine: Calcula tiempo virtual acelerado<br/>(baseDateTime + delta * factorTiempo)<br/>Zona: America/Montevideo (UTC-3)
    Engine ->> Core: calcularComandos(fechaSimulada, dataSite)
    activate Core
    Note over Core: Evalúa tarifa punta, déficit<br/>y límite potencia contratada
    Core -->> Engine: CoreAnswer (comandos ON/OFF)
    deactivate Core

    loop Por cada comando
        alt Switch en backoff (falló hace poco)
            Note over Engine: No se lo llama: el comando queda<br/>para un ciclo posterior
        else Comando ON
            Engine ->> State: Potencia de las habitaciones encendidas
            alt Con este ON se supera la potencia contratada
                Note over Engine: ON omitido (un OFF fallido sigue<br/>contando como encendido)
            else Entra en la contratada
                Engine ->> Switch: POST /switches/{id}/comandos (ON)
                Switch -->> Engine: 200 OK (estado ON)
                Engine ->> State: updateSwitchState(ENCENDIDO)
            end
        else Comando OFF
            Engine ->> Switch: POST /switches/{id}/comandos (OFF)
            Switch -->> Engine: 200 OK (estado OFF)
            Engine ->> Switch: GET /switches/{id} (confirmación)
            alt GET responde ON (el OFF no se aplicó)
                Note over Engine: Fallo: sigue ENCENDIDO,<br/>entra en backoff y se reintenta
            else GET responde OFF, o no se puede consultar
                Engine ->> State: updateSwitchState(APAGADO)
            end
        end
    end
    Note over Engine,Switch: Error HTTP o timeout: el estado no cambia y el switch<br/>entra en backoff (30 s simulados, duplicándose hasta 2 min)
    deactivate Engine
```

### Secuencia: Registro Inicial y Sincronización con el Simulador

```mermaid
sequenceDiagram
    autonumber
    participant App as Controlador (IntegrationService)
    participant Sim as Simulador de Cátedra
    participant InvService as InventoryService
    participant MqttSync as MqttSubscriptionService
    participant MqttSub as MqttSubscriber
    participant SiteState as SiteStateService
    Note over App: ApplicationReadyEvent
    loop Reintento cada 5 seg si no responde
        App ->> Sim: POST /controladores (etiqueta, baseUrl)
        Sim -->> App: 201 Created (idControlador, brokerMqtt, secret)
    end

    App ->> MqttSync: conectar(brokerUrl, clientId)
    MqttSync ->> InvService: obtener inventario activo
    InvService -->> MqttSync: habitaciones / topics
    MqttSync ->> MqttSub: connect(brokerUrl, clientId, topics)
    MqttSub -->> App: Conexión establecida y suscripción activa

    InvService -->> SiteState: replace(inventory, true/false)
    SiteState -->> InvService: estado runtime actualizado
    InvService ->> MqttSync: UpdatedInventoryEvent
    MqttSync ->> MqttSub: updateSubscriptions(topics)
```

---

## 4. Vista de Desarrollo

Organización estática de módulos Maven y responsabilidades en el repositorio:

```mermaid
graph TD
    subgraph Repo ["Estructura Maven Multimódulo"]
        Common["common<br/>(DTOs compartidos, MessageObject)"]
        Core["core<br/>(Lógica pura de negocio, TDD, cobertura ≥ 85%)"]
        Controlador["controller (controlador)<br/>(Spring Boot 3, API REST, Engine, Mongo, MQTT)"]
        SwitchesApi["switches-api<br/>(Stub local de switches para pruebas)"]
        Publisher["publisher<br/>(Generador de eventos MQTT para pruebas locales)"]
    end

    Controlador --> Core
    Controlador --> Common
    Publisher --> Common
```

### Distribución de Paquetes (`controlador`):

- `com.iiss.sensores.grupo_2.controlador.api`: Controladores REST (`EngineController`, `SiteController`,
  `RoomController`), entidades del inventario y servicios como `InventoryService` y `RoomService`.
- `com.iiss.sensores.grupo_2.controlador.engine.service`: `TemperatureControlEngine`, `SiteStateService`,
  `ReevaluationControlService`, `InventoryMapper` y soporte de estado del motor.
- `com.iiss.sensores.grupo_2.controlador.engine.mqtt`: `MqttSubscriber` con manejo de callbacks de conexión y
  reconexión.
- `com.iiss.sensores.grupo_2.controlador.engine.client`: `SwitchClient` y configuración de `RestClient` HTTP con
  timeouts y manejo de errores, backoff de reintentos en tiempo simulado y confirmación de los OFF con un GET.
- `com.iiss.sensores.grupo_2.controlador.integration`: `IntegrationService` y `MqttSubscriptionService`.

### Pruebas Automatizadas:

- **Unitarias TDD (`core`):** `CoreTest`, `RushHourTest` (cobertura superior al 85% requerida por la cátedra).
- **Integración (`controller/src/test/.../integration`):**
    - `ControlTempIntegrationIT`: Control bajo carga suficiente vs limitación efectiva de potencia.
    - `MqttResilienceIT`: Prueba de caída de broker Mosquitto en contenedor Testcontainers y reconexión transparente.
    - `SwitchClientRestFailureIT`: Validación ante timeouts o caídas del servicio REST de switches sin romper el loop de
      control.
    - `BigBangSystemIntegrationIT`: Prueba end-to-end con ciclo completo de arranque, simulación y parada.

---

## 5. Vista Física

Despliegue de nodos y contenedores Docker administrados por `docker/docker-compose.yml`:

```mermaid
graph TB
    subgraph Host ["Entorno Docker Compose"]
        subgraph C_Controlador ["Contenedor: controlador (Java 25)"]
            SpringApp["API REST :8080<br/>Engine MQTT<br/>Core Decisor"]
        end

        subgraph C_Mongo ["Contenedor: mongodb"]
            MongoEngine["MongoDB :27017<br/>(inventario y lecturas)"]
        end

        subgraph C_Broker ["Contenedor: mosquitto"]
            Mosquitto["Eclipse Mosquitto :1883"]
        end

        subgraph C_Switches ["Contenedor: switches-api (opcional / stub)"]
            SwitchStub["Switches Mock :8090"]
        end
    end

    subgraph Externo ["Entorno de Cátedra / Evaluación"]
        SimuladorExt["Simulador Externo<br/>(Broker MQTT + REST API)"]
    end

    C_Controlador -->|tcp://mongodb:27017| C_Mongo
    C_Controlador -.->|tcp://mosquitto:1883| C_Broker
    C_Controlador -.->|http://switches - api:8090| C_Switches
    C_Controlador ===|HTTP / MQTT| SimuladorExt
```

- En modo standalone local, `mosquitto` y `switches-api` brindan soporte a las pruebas manuales.
- En modo evaluación/simulador, las variables `CONECTAR_CON_SIMULADOR=true`, `SIMULATOR_API_URL` y `MQTT_BROKER_URL`
  redirigen la conectividad hacia el entorno del simulador de la cátedra.

---

## 6. Escenarios (+1) — Casos de Calidad y Resiliencia (Iteración 5)

La vista de escenarios concreta cómo responde el sistema ante los casos de calidad exigidos:

```mermaid
flowchart TD
    A[Lectura recibida o Timer de reevaluación] --> B{¿Controlador Activo?}
B -- No --> C[Ignorar o registrar solo en BD]
B -- Sí --> D[Calcular hora virtual = base + delta * factor]
D --> E[Obtener estado del sitio enriquecido]
E --> V[Apagar switches no controlables: habitaciones eliminadas<br/>y termostatos sin lecturas durante 15 min del reloj del control]
V --> F[Core: calcularComandos]
F --> G{¿Horario Punta en UTC-3?}
G -- Sí --> H[Generar comandos OFF para todos los switches]
G -- No --> I[Priorizar habitaciones con déficit térmico]
I --> J{¿Supera potencia contratada?}
J -- Sí --> K[Encender solo las de mayor déficit hasta el límite]
J -- No --> L[Encender todas las que requieran calor]
H --> M[Despachar comandos REST a Switches]
K --> M
L --> M
M --> Q{¿Switch en backoff?}
Q -- Sí --> R[Postergar el comando a un ciclo posterior]
Q -- No --> S{¿Es un ON que supera la contratada<br/>con los switches realmente encendidos?}
S -- Sí --> R
S -- No --> N{¿Falla Switch REST?}
N -- Sí --> O[Loggear error, mantener el último estado conocido,<br/>backoff en tiempo simulado y continuar con el resto]
N -- No --> T{¿Fue un OFF?}
T -- Sí --> U{GET de confirmación:<br/>¿sigue en ON?}
U -- Sí --> O
U -- "No, o no se puede consultar" --> P[Actualizar el estado del switch]
T -- No --> P
```

### Casos de Calidad Validados:

1. **Factor de Aceleración de Tiempo:**  
   Al enviar comando `INICIAR` con `{ "fechaHora": "...", "factorTiempo": 10 }`, el controlador escala el transcurso de
   los minutos reales por 10 al interactuar con el Core, permitiendo simular días completos en minutos.
2. **Caída y Recuperación de MQTT:**  
   Ante la desconexión del broker, `MqttSubscriber` retiene los tópicos deseados en memoria y activa el mecanismo de
   reconexión automática de Paho. Al restablecerse la red, re-suscribe de inmediato sin requerir reinicio del
   contenedor.
3. **Falla en Switches REST:**  
   Si una habitación reporta fallo de red o error 5xx al intentar conmutar su switch, `TemperatureControlEngine` aísla la
   excepción y prosigue aplicando los comandos del resto de los ambientes.
   - El estado del switch se actualiza solo cuando el comando tuvo éxito: un OFF fallido (F09) deja la habitación
     contando como encendida, y el motor no envía un ON que con ella supere la potencia contratada.
   - Un OFF que responde OK pero no apaga (F05) se detecta con un `GET /switches/{id}` posterior: si el switch sigue en
     ON se trata como fallo. Si el GET no se puede hacer (F08), se toma la respuesta del comando.
   - Un switch que falla entra en backoff medido en tiempo simulado: 30 s que se duplican hasta un tope de 2 min.
     Así no se lo reintenta en un bucle cerrado, pero vuelve a comandarse a pocos minutos de recuperarse aunque el
     reloj esté acelerado. Un cambio de configuración del sitio o un nuevo inicio del controlador reinician el
     backoff.
4. **Límite de Potencia Eléctrica:**  
   Si tres habitaciones necesitan calefacción simultánea pero encender las tres excede los kW contratados del sitio, el
   Core prioriza estrictamente las que se encuentran más lejos de su temperatura esperada, preservando la seguridad
   eléctrica del hogar.
5. **Termostato que deja de publicar (F03):**  
   Una habitación con el switch encendido cuyo termostato no envía lecturas durante
   `controlador.lectura-max-antiguedad-min` minutos (15 por defecto) no se puede controlar con seguridad, así que el
   motor la apaga al comienzo del ciclo. El silencio se mide con el reloj del control (simulado y acelerado por el
   `factorTiempo`), no con el reloj del sistema: con factor 60, 15 minutos sin lecturas son 15 segundos reales.
   Mientras el controlador está detenido se usa el reloj del sistema; al iniciar o detener, las marcas ya registradas
   se trasladan al nuevo reloj conservando el tiempo transcurrido. Con el mismo reloj se mide el plazo (1 h) tras el
   cual se deja de reintentar el apagado del switch de una habitación eliminada.
