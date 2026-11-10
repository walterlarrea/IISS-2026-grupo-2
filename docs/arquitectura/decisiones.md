# Registro de decisiones de arquitectura — EcoWarm

Cada decisión sigue el formato **Contexto → Decisión → Consecuencias**.

| # | Título | Estado |
|---|---|---|
| [001](#001-proyecto-maven-multimódulo) | Proyecto Maven multimódulo | Aceptada |
| [002](#002-core-de-decisión-en-java-puro) | Core de decisión en Java puro | Aceptada |
| [003](#003-unificar-subscriber-y-rooms-api-en-controller) | Unificar subscriber y rooms-api en "controller" | Aceptada |
| [004](#004-mqtt-para-lecturas-rest-para-switches) | MQTT para lecturas, REST para switches | Aceptada |
| [005](#005-mongodb-para-inventario-y-lecturas) | MongoDB para inventario y lecturas | Aceptada |
| [006](#006-reloj-del-control-virtual-y-acelerado) | Reloj del control virtual y acelerado | Aceptada |
| [007](#007-control-por-ciclo-evento--reevaluación-periódica) | Control por ciclo: evento + reevaluación periódica | Aceptada |
| [008](#008-prioridad-por-déficit-con-límite-duro-de-potencia) | Prioridad por déficit con límite duro de potencia | Aceptada |
| [009](#009-histéresis-de-encendido) | Histéresis de encendido | Aceptada |
| [010](#010-anti-starvation-y-tiempo-mínimo-encendida) | Anti-starvation y tiempo mínimo encendida | Aceptada |
| [011](#011-ceder-potencia-por-inercia-térmica) | Ceder potencia por inercia térmica | Aceptada |
| [012](#012-el-estado-de-un-switch-solo-cambia-con-éxito-confirmado) | El estado de un switch solo cambia con éxito confirmado | Aceptada |
| [013](#013-backoff-de-switches-en-tiempo-simulado) | Backoff de switches en tiempo simulado | Aceptada |
| [014](#014-apagar-lo-que-no-se-puede-controlar) | Apagar lo que no se puede controlar | Aceptada |
| [015](#015-servidor-mcp-como-módulo-separado) | Servidor MCP como módulo separado | Aceptada |
| [016](#016-api-key-estática-por-header) | API key estática por header | Aceptada |

---

## 001: Proyecto Maven multimódulo
**Contexto.** El sistema tiene varios ejecutables (controlador, generador de lecturas, stub de switches, servidor MCP) que comparten DTOs.
**Decisión.** Un `pom.xml` raíz que agrega los módulos `core`, `common`, `controller`, `publisher`, `switches-api` y `mcp-server`, ubicados en la raíz del repositorio (equivalen al `/modulos/` del enunciado).
**Consecuencias.** Un solo `mvn verify` compila y prueba todo; un único `Dockerfile` multi-stage genera una imagen por módulo (`target:`).

## 002: Core de decisión en Java puro
**Contexto.** Las reglas de negocio (tarifa, potencia, prioridades) deben poder probarse sin broker, base de datos ni red.
**Decisión.** El módulo `core` expone `ICore.calcularComandos(ZonedDateTime, DataSite) : CoreAnswer`, sin Spring ni dependencias de infraestructura (solo SLF4J). Recibe una "snapshot" del sitio y devuelve comandos; no tiene estado entre llamadas.
**Consecuencias.** Se desarrolló con TDD y alta cobertura (`CoreTest`, `RushHourTest`). Todo lo que necesita "memoria" (último cambio de switch, lecturas) lo aporta el engine en el `DataSite`.

## 003: Unificar subscriber y rooms-api en `controller`
**Contexto.** En iteraciones tempranas la recepción MQTT y la API de habitaciones eran servicios separados que compartían la misma base y debían coordinarse.
**Decisión.** Un solo servicio Spring Boot `controller` con paquetes `api`, `engine` e `integration`.
**Consecuencias.** Menos despliegue y sin llamadas internas por red. La API `/habitaciones` heredada sigue existiendo pero el control usa el inventario de `/sitio` (ver [roadmap P2](roadmap.md)).

## 004: MQTT para lecturas, REST para switches
**Contexto.** Los termostatos publican, los switches se comandan.
**Decisión.** Eclipse Paho con reconexión automática para MQTT (QoS 1, `cleanSession=true`) y `RestClient` con timeouts para switches.
**Consecuencias.** Las lecturas publicadas con el broker caído se pierden; se acepta porque la siguiente lectura reemplaza a la anterior.

## 005: MongoDB para inventario y lecturas
**Contexto.** Hay que persistir el inventario activo y las lecturas, con esquema flexible.
**Decisión.** MongoDB (colecciones `inventario_controlador`, `Mediciones`).
**Consecuencias.** Al reiniciar se recupera la última lectura por termostato. El estado de runtime (backoff, marcas) no se persiste.

## 006: Reloj del control virtual y acelerado
**Contexto.** El simulador corre con tiempo acelerado (`factorTiempo`).
**Decisión.** `CustomClock` calcula `fechaHora = base + (ahora − inicio) × factorTiempo` en `America/Montevideo`.
**Todo** plazo del controlador (antigüedad de lecturas, backoff, espera máxima, mínimo encendida, habitaciones eliminadas) se mide con ese reloj. Al iniciar o detener, las marcas ya registradas se trasladan al nuevo reloj conservando el tiempo transcurrido.
**Consecuencias.** El comportamiento es el mismo con factor 1 o 60. Mientras el control está detenido se usa el reloj del sistema.

## 007: Control por ciclo: evento + reevaluación periódica
**Contexto.** Si un termostato deja de publicar, no habría nuevas decisiones.
**Decisión.** Cada lectura dispara un ciclo, y `ReevaluationControlService` (`@Scheduled`, `controlador.reevaluacion-ms`) dispara ciclos aunque no lleguen lecturas.
**Consecuencias.** Las reglas basadas en tiempo (tarifa punta, anti-starvation, lecturas viejas) se aplican aunque no haya tráfico.

## 008: Prioridad por déficit con límite duro de potencia
**Contexto.** La potencia contratada no alcanza para encender todo.
**Decisión.** Primero se apagan las habitaciones satisfechas, luego se recorta exceso (*shed overload*, menor déficit primero) y por último se encienden por mayor déficit `T_esperada − T_actual` mientras entre en la contratada. La contratada es un límite duro: se respeta aun contra el tiempo mínimo encendida. El engine vuelve a verificarla con los switches realmente encendidos antes de cada ON.
**Consecuencias.** Nunca se supera la contratada, a costa de que las habitaciones con poco déficit esperen (mitigado por 010).

## 009: Histéresis de encendido
**Contexto.** Con un único umbral, un switch se prendía y apagaba en cada lectura alrededor del objetivo.
**Decisión.** Una habitación apagada solo se considera para encender si está `CONTROLADOR_HISTERESIS_ENCENDIDO_C` (0,3 °C) por debajo del objetivo; una encendida sigue calentando hasta llegar al objetivo.
**Consecuencias.** Menos conmutaciones; la temperatura oscila dentro de esa banda. Con `0` se desactiva.

## 010: Anti-starvation y tiempo mínimo encendida
**Contexto.** Con prioridad estricta por déficit, una habitación con poco déficit podía no encenderse nunca.
**Decisión.** Una habitación apagada y bajo su objetivo que espera más de `CONTROLADOR_ESPERA_MAXIMA_MIN` (60 min) pasa primero, desalojando habitaciones encendidas (menor déficit primero) solo si así entra. Una habitación recién encendida está protegida `CONTROLADOR_MINIMO_ENCENDIDA_MIN` (10 min) para no oscilar. El tiempo forzado apagado por tarifa punta no cuenta como espera.
**Consecuencias.** Todas las habitaciones reciben calor eventualmente.

## 011: Ceder potencia por inercia térmica
**Contexto.** La losa radiante sigue entregando calor un rato después de apagarse.
**Decisión.** Si una habitación fría no entra, las habitaciones encendidas que ya están dentro de `CONTROLADOR_INERCIA_TERMICA_C` (0,1 °C) de su objetivo y tienen menos déficit le ceden el switch, solo si juntas liberan lo suficiente.
**Consecuencias.** Mejor reparto de potencia. El valor es global (ver [roadmap L3](roadmap.md)).

## 012: El estado de un switch solo cambia con éxito confirmado
**Contexto.** Un comando OFF fallido o un OFF que respondía OK sin apagar hacían que el controlador creyera tener potencia libre y superara la contratada (F05, F06, F09).
**Decisión.** El estado local de un switch solo cambia si el comando tuvo éxito. Cada OFF exitoso se confirma con `GET /switches/{id}`: si sigue ON es un fallo; si el GET no responde se toma la respuesta del comando. Un switch en estado `DESCONOCIDO` se lleva a OFF.
**Consecuencias.** Un OFF fallido sigue contando como potencia usada; se agrega un GET por cada OFF.

## 013: Backoff de switches en tiempo simulado
**Contexto.** Reintentar un switch caído en cada ciclo genera bucles y demora el resto.
**Decisión.** Un switch que falla no se vuelve a llamar por 30 s simulados, duplicándose hasta 2 min. Un cambio del sitio o un nuevo INICIAR reinician el backoff.
**Consecuencias.** Con reloj acelerado un switch recuperado vuelve a comandarse en pocos minutos simulados. Los valores son constantes en `SwitchClient`.

## 014: Apagar lo que no se puede controlar
**Contexto.** Una habitación sin lecturas recientes (F03) o eliminada del sitio con su switch encendido quedaría calentando sin control.
**Decisión.** Al comienzo de cada ciclo se apagan los switches de termostatos sin lecturas durante `controlador.lectura-max-antiguedad-min` (15 min de reloj del control) y los de habitaciones eliminadas; a estas últimas se les reintenta el apagado durante 1 h.
**Consecuencias.** Se prioriza la seguridad sobre el confort.

## 015: Servidor MCP como módulo separado
**Contexto.** Se quería permitir que asistentes de IA consulten y operen el sistema.
**Decisión.** Módulo `mcp-server` (Spring AI) que consume la API REST del controlador, no accede a la base directamente.
**Consecuencias.** El controlador no depende de la IA. Comparación de enfoques en [`docs/mcp/rest-vs-mcp.md`](../mcp/rest-vs-mcp.md).

## 016: API key estática por header
**Contexto.** El protocolo del simulador requiere autenticar las llamadas.
**Decisión.** Header `X-API-Key` (`CONTROLADOR_API_KEY`, `SWITCHES_API_KEY`). **Consecuencias.** Simple, suficiente para el prototipo; no apta para producción.
