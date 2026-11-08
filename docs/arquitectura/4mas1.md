# Arquitectura 4+1 — EcoWarm (Versión Final del Sistema)

**Proyecto:** EcoWarm — Sistema de control de temperatura domótico  
**Grupo:** Grupo 2  
**Alcance:** Arquitectura de Producción / Entrega Final (Algoritmo Multi-fase, Anti-Starvation, Histéresis, Resiliencia y Tiempo Acelerado)

---

## 1. Introducción y Contexto

El sistema **EcoWarm** gestiona de forma autónoma, eficiente y segura la climatización por losa radiante de un sitio inteligente (hogares, oficinas o edificios). La arquitectura se fundamenta en un desacoplamiento estricto entre el motor de decisión algorítmico y la infraestructura de comunicaciones, incorporando mecanismos avanzados de control térmico y resiliencia ante contingencias del mundo real:

- **Algoritmo de Control en 6 Fases:** Gestión desacoplada que resuelve tarifa punta, deslastre de sobrecarga, habitaciones postergadas (*anti-starvation*), aprovechamiento de calor residual por inercia térmica, histéresis de encendido y priorización por déficit térmico bajo el límite estricto de potencia contratada ($kW$).
- **Tiempo Acelerado y Virtualizado con Lookahead:** Soporte para simulaciones de alta velocidad (factor de aceleración $X$) con proyección preventiva de ciclos para no iniciar calefacción justo antes de una tarifa punta.
- **Resiliencia Integral y Tolerancia a Fallos:** 
  - Reconexión transparente ante caídas del broker MQTT (Paho automático con persistencia de tópicos).
  - Circuito de *Backoff* y aislamiento de excepciones por dispositivo en la API REST de switches.
  - Fail-safe ante estados desconocidos (`DESCONOCIDO` $\rightarrow$ apagado forzado).
- **Seguridad Operacional Dinámica:** Apagado automático de switches huérfanos (habitaciones eliminadas/modificadas del inventario) y de switches con termostatos silenciosos (*stale* sin lecturas recientes).
- **Filtro de Telemetría Anómala:** Descarte de lecturas no plausibles ($< -10\ ^\circ\text{C}$ o $> 50\ ^\circ\text{C}$, NaN, infinitos).
- **Seguridad de Acceso:** Autenticación de endpoints y clientes mediante encabezado `X-API-Key`.

---

## 2. Vista Lógica

La vista lógica desglosa la funcionalidad del sistema en módulos altamente cohesivos y con acoplamiento débil, separando la lógica de decisión pura del framework de aplicación y los adaptadores de infraestructura.

```mermaid
graph TD
    subgraph CoreModule ["Módulo Core (Java Puro - Dominio Desacoplado)"]
        ICore["«interface»<br/>ICore"]
        CoreImpl["Core<br/>(Decisor Multi-fase)"]
        DataSite["DataSite (Snapshot)"]
        Room["Room"]
        RushHour["RushHour / Fee"]
        CycleState["CycleState<br/>(Balance de Potencia)"]
        CoreAnswer["CoreAnswer"]
        CommandSwitch["CommandSwitch (ON / OFF)"]

        CoreImpl -.->|implements| ICore
        CoreImpl --> CycleState
        CoreImpl --> DataSite
        DataSite --> Room
        CoreImpl --> RushHour
        CoreImpl --> CoreAnswer
        CoreAnswer --> CommandSwitch
    end

    subgraph ControllerModule ["Módulo Controlador / Engine (Spring Boot)"]
        EngineCtrl["EngineController (REST)"]
        SiteCtrl["SiteController / RoomController"]
        ApiKeyFilter["ApiKeyRoom (Filtro X-API-Key)"]
        
        TempEngine["TemperatureControlEngine<br/>(Orquestador Principal)"]
        CustomClock["CustomClock<br/>(Virtualizador de Tiempo)"]
        SiteState["SiteStateService<br/>(Estado en Memoria e Hidratación)"]
        ReevalService["ReevaluationControlService<br/>(@Scheduled cada 5s)"]
        
        MqttSub["MqttSubscriber (Eclipse Paho)"]
        MqttSync["MqttSubscriptionService"]
        Integration["IntegrationService<br/>(Handshake con Simulador)"]
        InvService["InventoryService"]
        SwitchCli["SwitchClient<br/>(REST con Backoff y API Key)"]
        MongoRepo["TemperatureReadingRepository"]

        EngineCtrl --> ApiKeyFilter
        SiteCtrl --> ApiKeyFilter
        EngineCtrl --> TempEngine
        SiteCtrl --> SiteState
        SiteCtrl --> InvService
        InvService -.->|UpdatedInventoryEvent| SiteState
        InvService -.->|UpdatedInventoryEvent| MqttSync

        TempEngine --> CustomClock
        TempEngine --> SiteState
        TempEngine --> ICore
        TempEngine --> SwitchCli
        
        ReevalService --> TempEngine
        MqttSub --> MongoRepo
        MqttSub --> TempEngine
        Integration --> InvService
        Integration --> MqttSync
        MqttSync --> MqttSub
    end

    subgraph ExternalServices ["Servicios Externos / Infraestructura"]
        SimSimulator["Simulador de Cátedra / Evaluación"]
        MqttBroker["Broker MQTT (Mosquitto)"]
        SwitchesAPI["API REST Switches"]
        MongoDB[("MongoDB (Persistencia)")]
    end

    Integration -.->|POST /controladores<br/>GET /sitio| SimSimulator
    MqttBroker -.->|Telemetría tópicos| MqttSub
    SwitchCli -.->|POST ON/OFF con X-API-Key| SwitchesAPI
    MongoRepo -.->|Lecturas históricas| MongoDB
    SiteState -.->|Hydration inicial| MongoRepo
```

### Componentes y Responsabilidades:

#### 1. Módulo Core (`core` — Java 25 puro):
- **Cero dependencias externas:** No depende de Spring, MQTT, HTTP ni bases de datos.
- **Contrato unificado:** `ICore.calcularComandos(ZonedDateTime fechaHora, DataSite dataSite) : CoreAnswer`.
- **Estrategia de Decisión en 6 Fases:**
  1. **Tarifa Punta (`RushHour`):** Apagado general preventivo si la hora evaluada corresponde a horario pico en día hábil (UTC-3).
  2. **Apagado de Satisfechas:** Apaga switches de habitaciones cuya temperatura actual alcanza o supera la esperada ($T_{actual} \ge T_{esperada}$), liberando potencia inmediatamente.
  3. **Deslastre de Sobrecarga (*Shed Overload*):** Si la potencia activa actual supera la contratada, apaga selectivamente las habitaciones menos necesitadas para evitar sobrecargas eléctricas.
  4. **Atención de Inanición (*Anti-Starvation*):** Si una habitación con switch apagado lleva más de `DEFAULT_MAX_WAIT` (60 min) esperando por debajo de su consigna, es promovida de forma prioritaria, pudiendo expropiar potencia a habitaciones activas que ya hayan cumplido su tiempo mínimo encendidas (`DEFAULT_MIN_ON_TIME` = 10 min).
  5. **Cesión por Inercia Térmica:** Si una habitación está muy cerca de su objetivo (a menos de $\Delta = 0.1\ ^\circ\text{C}$) y ya cumplió con el tiempo mínimo de encendido, apaga preventivamente su switch para ceder su potencia a habitaciones más frías, aprovechando el calor residual de la losa radiante.
  6. **Encendido por Déficit con Histéresis:** Ordena las habitaciones frías por mayor brecha térmica $(T_{esperada} - T_{actual})$. Solo se encienden aquellas que estén al menos `DEFAULT_TURN_ON_HYSTERESIS_C` ($0.3\ ^\circ\text{C}$) por debajo del objetivo (evitando ciclado rápido o *chattering*), y hasta el límite estricto de la potencia contratada del sitio.

#### 2. Módulo Controlador / Engine (`controller` — Spring Boot 3):
- **`TemperatureControlEngine`:** Corazón orquestador. Administra el ciclo de vida del controlador (`start` / `stop`), calcula el tiempo virtual con `CustomClock`, aplica el *lookahead* de tarifa punta (`timeForCore`), invoca el `ICore`, despacha los comandos a través de `SwitchClient` con manejo aislado de fallas, y comanda apagado de emergencia para switches huérfanos y estancados. Al detenerse el motor, apaga preventivamente todos los switches activos.
- **`SiteStateService`:** Mantiene el estado en memoria viva del sitio. Implementa hidratación perezosa (*lazy hydration*) desde MongoDB y la API de switches al inicio o ante modificaciones de inventario. Supervisa termostatos silenciosos (*stale* sin lecturas por $> 15$ min) y gestiona switches huérfanos (*orphans*).
- **`CustomClock`:** Encapsula la virtualización temporal, aplicando el factor de aceleración sobre el reloj base del sistema.
- **`SwitchClient`:** Cliente HTTP con soporte de `X-API-Key`, detección y mitigación de fallos vía *backoff* exponencial/progresivo (`SwitchInBackoffException`), y corrección de switches en estado `DESCONOCIDO` mediante orden inmediata de apagado.
- **`ReevaluationControlService`:** Disparador periódico (`@Scheduled` cada 5 s) que fuerza ciclos de evaluación en `TemperatureControlEngine`, asegurando que las reglas basadas en tiempo (expiración de esperas, inercia térmica y hora punta) se ejecuten puntualmente aunque ningún sensor envíe lecturas.

---

## 3. Vista de Procesos

La vista de procesos ilustra la concurrencia, el manejo de eventos asíncronos y la resiliencia operativa en los distintos hilos de ejecución.

### 3.1 Recepción de Lectura MQTT, Filtrado y Ciclo de Control

```mermaid
sequenceDiagram
    autonumber
    actor Sensor as Termostato / Sensor MQTT
    participant Broker as Broker Mosquitto
    participant MqttSub as MqttSubscriber (Hilo MQTT)
    participant Engine as TemperatureControlEngine
    participant State as SiteStateService
    participant Clock as CustomClock
    participant Core as Core (ICore)
    participant SwitchCli as SwitchClient
    participant SwitchAPI as API REST Switch

    Sensor ->> Broker: Publica {"tC": 18.5, "idTermostato": "T-1"}
    Broker ->> MqttSub: messageArrived(topic, payload)
    
    alt Lectura Anómala (tC < -10 o > 50 o NaN)
        MqttSub ->> MqttSub: Descartar telemetría y loguear advertencia
    else Lectura Válida
        MqttSub ->> State: updateRoomTemperature("T-1", 18.5)
        MqttSub ->> Engine: execute() (sincronizado)
        activate Engine
        
        Engine ->> Clock: getTimeZoned()
        Clock -->> Engine: simulatedDateTime (acelerado)
        
        Engine ->> State: getSiteState(simulatedDateTime)
        State -->> Engine: dataSite (con temperaturas y estados de switches)
        
        Engine ->> Engine: turnOffUncontrollableSwitches(orphans, stale)
        Note over Engine: timeForCore: proyecta si el próximo ciclo<br/>cae en Horario Punta (anticipación)
        
        Engine ->> Core: calcularComandos(simulatedTime, dataSite)
        activate Core
        Note over Core: Fases: Punta -> Apagado -> Sobrecarga<br/>-> Anti-Inanición -> Inercia -> Histéresis
        Core -->> Engine: CoreAnswer (comandos ON / OFF)
        deactivate Core
        
        loop Por cada comando CommandSwitch
            alt Switch en período de Backoff
                Engine ->> Engine: Pospone comando (evita saturar switch caído)
            else Switch listo
                Engine ->> SwitchCli: encender(room) / apagar(room) con X-API-Key
                activate SwitchCli
                SwitchCli ->> SwitchAPI: POST /switches/{id}/comandos
                alt Respuesta Exitosa (200 OK)
                    SwitchAPI -->> SwitchCli: 200 OK
                    SwitchCli -->> Engine: OK
                    Engine ->> State: updateSwitchState(id, estado, simulatedDateTime)
                else Error de Red o 5xx
                    SwitchAPI -->> SwitchCli: Timeout / 500 Error
                    SwitchCli ->> SwitchCli: Registrar falla y activar Backoff
                    SwitchCli -->> Engine: Error capturado y aislado
                    Note over Engine: Continúa con los demás switches sin abortar
                end
                deactivate SwitchCli
            end
        end
        deactivate Engine
    end
```

### 3.2 Reevaluación Periódica: Anti-Inanición e Inercia Térmica

```mermaid
sequenceDiagram
    autonumber
    participant Timer as ReevaluationControlService (@Scheduled 5s)
    participant Engine as TemperatureControlEngine
    participant Clock as CustomClock
    participant State as SiteStateService
    participant Core as Core (ICore)
    participant SwitchCli as SwitchClient

    Timer ->> Engine: execute()
    activate Engine
    Note over Engine: Se ejecuta periódicamente aún sin nuevas lecturas MQTT
    Engine ->> Clock: getTimeZoned()
    Clock -->> Engine: simulatedDateTime
    Engine ->> State: getSiteState(simulatedDateTime)
    State -->> Engine: dataSite
    
    Engine ->> Core: calcularComandos(simulatedDateTime, dataSite)
    activate Core
    Note over Core: Detecta habitación que esperó > 60m (Starvation)<br/>o habitación en objetivo por inercia (Cesión)
    Core -->> Engine: CoreAnswer (Comandos de compensación)
    deactivate Core
    
    Engine ->> SwitchCli: Despacha comandos (ON / OFF)
    deactivate Engine
```

### 3.3 Autoconfiguración y Registro con el Simulador Externo

```mermaid
sequenceDiagram
    autonumber
    participant App as IntegrationService (ApplicationReadyEvent)
    participant Sim as Simulador de Cátedra
    participant Inv as InventoryService
    participant State as SiteStateService
    participant MqttSync as MqttSubscriptionService
    participant MqttSub as MqttSubscriber

    Note over App: Arranque de Spring Boot
    loop Reintento con backoff hasta disponibilidad
        App ->> Sim: POST /controladores (etiqueta, baseUrl)
        Sim -->> App: 201 Created (idControlador, brokerMqtt, secret)
    end

    App ->> Sim: GET /sitio
    Sim -->> App: 200 OK (inventario, tarifa, habitaciones)
    
    App ->> Inv: updateSite(dataSite)
    Inv ->> State: loadSiteState(dataSite) (Hidratación perezosa)
    Inv ->> MqttSync: onUpdatedInventory(event)
    MqttSync ->> MqttSub: subscribeToTopics(termostatos del sitio)
    MqttSub -->> App: Suscripción MQTT completada
```

---

## 4. Vista de Desarrollo

Muestra la modularización física del código fuente en Maven, la separación de responsabilidades y la estrategia de calidad.

```mermaid
graph TD
    subgraph MultiModule ["Estructura Maven Multimódulo (Java 25)"]
        Common["common<br/>• DTOs (TemperatureMessage, MessageObject)<br/>• Enums de dominio compartidos"]
        Core["core<br/>• Algoritmo de decisión desacoplado<br/>• Dominio puro (TDD, cobertura ≥ 85%)<br/>• Modelos de entrada/salida"]
        Controller["controller<br/>• Aplicación Spring Boot 3<br/>• API REST con X-API-Key<br/>• Engine de Control y CustomClock<br/>• Cliente MQTT Eclipse Paho<br/>• SwitchClient con Backoff<br/>• Repositorios MongoDB"]
        SwitchesApi["switches-api<br/>• Stub local de Switches para pruebas"]
        RoomsApi["rooms-api<br/>• API auxiliar de catálogo de habitaciones"]
        Publisher["publisher<br/>• Generador simulado de eventos MQTT"]
    end

    Controller --> Core
    Controller --> Common
    Publisher --> Common
    SwitchesApi --> Common
    RoomsApi --> Common
```

### Distribución de Paquetes (`controller`):

- `com.iiss.sensores.grupo_2.controlador.api`:
  - `controller`: `EngineController`, `SiteController`, `RoomController`.
  - `config`: `ApiKeyRoom` (seguridad de endpoints mediante `X-API-Key`).
  - `service`: `InventoryService`, `RoomService`.
- `com.iiss.sensores.grupo_2.controlador.engine`:
  - `service`: `TemperatureControlEngine`, `SiteStateService`, `ReevaluationControlService`.
  - `client`: `SwitchClient`, `SwitchRestClientConfig` (timeouts y *backoff*).
  - `mqtt`: `MqttSubscriber` (manejo de conexión, desconexión y retención de tópicos).
  - `utils`: `CustomClock` (manejo de tiempo acelerado virtual).
  - `persistence`: `TemperatureReadingRepository` (acceso a MongoDB).
- `com.iiss.sensores.grupo_2.controlador.integration`:
  - `IntegrationService`: Registro inicial ante el simulador externo.
  - `MqttSubscriptionService`: Gestión dinámica de suscripción a tópicos MQTT.

### Estrategia de Pruebas Automatizadas:

- **Módulo `core` (Pruebas Unitarias TDD Puras):**
  - `CoreTest`: Cobertura exhaustiva de las 6 fases de decisión: tarifa punta, histéresis ($0.3\ ^\circ\text{C}$), inercia térmica temprana ($0.1\ ^\circ\text{C}$), *anti-starvation* (espera $> 60$ min, retención mínima de $10$ min), expropiación y deslastre de sobrecarga de potencia contratada.
  - Cobertura de código certificada con JaCoCo $> 85\%$.
- **Módulo `controller` (Pruebas de Integración con Testcontainers y Mocks):**
  - `ControlTempIntegrationIT`: Control bajo carga térmica y validación de conmutación.
  - `MqttResilienceIT`: Caída y reinicio dinámico de contenedor Mosquitto mediante Testcontainers, verificando reconexión transparente sin pérdida de suscripción.
  - `SwitchClientRestFailureIT`: Tolerancia a errores HTTP 500, timeouts y activación de *backoff* en switches sin interrumpir el control de los demás ambientes.
  - `ApiKeyTest`: Verificación de rechazo de peticiones no autenticadas (401 Unauthorized).

---

## 5. Vista Física / Despliegue

La vista física describe la topología de contenedores y nodos de ejecución. Admite dos perfiles de funcionamiento: **Modo Local Autónomo** (para desarrollo y pruebas de integración) y **Modo Simulación / Evaluación de Cátedra**.

```mermaid
graph TB
    subgraph DockerHost ["Entorno Docker Compose (Host Local / CI)"]
        subgraph C_Controller ["Contenedor: controller (Java 25 - Spring Boot)"]
            AppEngine["Controlador EcoWarm<br/>• API REST :8080<br/>• Engine de Control<br/>• Core Decisor<br/>• Paho MQTT Client"]
        end

        subgraph C_Mongo ["Contenedor: mongodb"]
            MongoEngine["MongoDB :27017<br/>• db: sen_data<br/>• Inventario y Lecturas"]
        end

        subgraph C_LocalBroker ["Contenedor: mosquitto (Modo Local)"]
            MosquittoLocal["Eclipse Mosquitto :1883<br/>WebSocket :9001"]
        end

        subgraph C_LocalSwitches ["Contenedor: switches-api (Stub Local)"]
            SwitchesStub["Switches Stub :8090"]
        end
    end

    subgraph EvaluationHost ["Entorno de Cátedra / Evaluación Remota"]
        CatedraSim["Simulador de Cátedra<br/>• API REST Controladores & Sitio<br/>• API Switches Reales/Simulados"]
        CatedraBroker["Broker MQTT de Evaluación (:1883)"]
    end

    C_Controller -->|tcp://mongodb:27017| C_Mongo
    C_Controller -.->|Modo Local: tcp://mosquitto:1883| C_LocalBroker
    C_Controller -.->|Modo Local: http://switches-api:8090| C_LocalSwitches
    C_Controller ===|Modo Cátedra: REST HTTP| CatedraSim
    C_Controller ===|Modo Cátedra: MQTT :1883| CatedraBroker
```

- **Variables de Configuración (`.env.local` / `.env.dev`):**
  - `CONECTAR_CON_SIMULADOR`: Activa el modo evaluación contra la cátedra (`true`) o el entorno local de contenedores (`false`).
  - `SIMULATOR_API_URL` y `MQTT_BROKER_URL`: Endpoints configurables de la cátedra o locales.
  - `CONTROLLER_API_KEY` y `SWITCHES_API_KEY`: Claves criptográficas para asegurar los canales REST.

---

## 6. Vista de Escenarios (+1) — Casos de Calidad y Resiliencia

Los escenarios unen y validan las cuatro vistas anteriores a través del flujo integral de ejecución del ciclo de control:

```mermaid
flowchart TD
    Start([Disparador: Lectura MQTT o Timer @Scheduled]) --> Activo{¿Controlador en<br/>ejecución?}
    Activo -- No --> SaveOnly[Actualizar temperatura en memoria/BD y finalizar]
    Activo -- Sí --> ValidTemp{¿Temperatura<br/>plausible?<br/>-10°C ≤ tC ≤ 50°C}
    
    ValidTemp -- No --> Discard[Descartar lectura anómala y advertir en log]
    ValidTemp -- Sí --> CalcTime[Calcular hora virtual acelerada con CustomClock]
    
    CalcTime --> CleanSwitches[Apagar switches huérfanos o con termostatos stale]
    CleanSwitches --> Lookahead{¿Próximo ciclo cae<br/>en Horario Punta?}
    
    Lookahead -- Sí --> RushHourAction[Core: Generar APAGADO general preventivo]
    Lookahead -- No --> CoreLogic[Core: Evaluar estado térmico del sitio]
    
    CoreLogic --> PhaseOff[Fase 1: Apagar habitaciones que alcanzaron consigna]
    PhaseOff --> PhaseShed[Fase 2: Deslastrar sobrecarga si potencia activa > contratada]
    PhaseShed --> PhaseStarv{¿Hay habitaciones con<br/>espera > 60 min?}
    
    PhaseStarv -- Sí --> PromoteStarved[Fase 3: Promover y encender starved<br/>expropiando a habitaciones con ON ≥ 10 min]
    PhaseStarv -- No --> PhaseInertia
    PromoteStarved --> PhaseInertia
    
    PhaseInertia{¿Habitación a ≤ 0.1°C del objetivo<br/>y con encendido ≥ 10 min?}
    PhaseInertia -- Sí --> YieldPower[Fase 4: Apagar por inercia térmica<br/>y ceder potencia a más frías]
    PhaseInertia -- No --> PhaseDeficit
    YieldPower --> PhaseDeficit
    
    PhaseDeficit[Fase 5: Ordenar habitaciones frías por mayor déficit]
    PhaseDeficit --> CheckHysteresis{¿Déficit ≥ 0.3°C<br/>y potencia contratada<br/>disponible?}
    
    CheckHysteresis -- Sí --> CmdON[Generar comando ENCENDIDO]
    CheckHysteresis -- No --> CmdIgnore[Mantener apagada sin ciclado rápido]
    
    RushHourAction --> Dispatch[Despachar comandos a SwitchClient]
    CmdON --> Dispatch
    CmdIgnore --> Dispatch
    
    Dispatch --> CheckBackoff{¿Switch en Backoff<br/>por falla previa?}
    CheckBackoff -- Sí --> Postpone[Posponer comando al próximo ciclo]
    CheckBackoff -- No --> SendCmd[Enviar POST /comandos con X-API-Key]
    
    SendCmd --> CmdResult{¿Respuesta 200 OK?}
    CmdResult -- Sí --> UpdateState[Actualizar estado de switch en SiteStateService]
    CmdResult -- No --> RegFailure[Aislar error, registrar falla y activar Backoff]
    
    UpdateState --> End([Fin del Ciclo])
    RegFailure --> End
    Postpone --> End
    Discard --> End
    SaveOnly --> End
```

### Síntesis de Casos de Calidad Validados:

1. **Eficiencia y Confort Térmico (Histéresis e Inercia):**  
   Se previene el desgaste de relés y switches gracias a la histéresis de $0.3\ ^\circ\text{C}$. A su vez, la banda de inercia de $0.1\ ^\circ\text{C}$ aprovecha la energía remanente acumulada en la losa radiante, liberando potencia para habitaciones con mayor déficit.
2. **Equidad y Prevención de Inanición (*Anti-Starvation*):**  
   Ninguna habitación queda desatendida indefinidamente debido a la limitación de potencia contratada. Cumplidos $60$ minutos de espera bajo la temperatura esperada, el motor expropia potencia a zonas activas con más de $10$ minutos de funcionamiento continuo.
3. **Resiliencia Operativa de Red y Dispositivos:**  
   - Si la red o el servicio de switches falla, el circuito de *Backoff* evita la degradación por reintentos masivos y aísla la falla para no interrumpir el control de los demás ambientes.
   - Ante lecturas de estado `DESCONOCIDO` en switches, se envía una orden fail-safe de apagado.
   - Si el broker Mosquitto se desconecta, el cliente Eclipse Paho reconecta y re-suscribe automáticamente a los tópicos retenidos en memoria sin requerir reinicio del sistema.
4. **Protección Preventiva de Tarifas Eléctricas:**  
   El motor proyecta el avance del tiempo acelerado (`timeForCore`) y evita encender calefactores segundos antes de la entrada a la tarifa punta de UTE, previniendo penalizaciones económicas innecesarias.
5. **Seguridad Operacional Dinámica:**  
   La detección de termostatos silenciosos (*stale* $> 15$ min) y switches huérfanos garantiza que ninguna losa radiante continúe consumiendo energía fuera de supervisión.
