# PROYECTO DE SENSORES VERSION 5.

## Google Java Style

> Este proyecto sigue las convenciones de codificación oficiales de Google. Para más detalles, consulta
> la [Guía de Estilo de Java de Google (Google Java Style Guide)](https://google.github.io/styleguide/javaguide.html)

## Estilo de Código

Acentuando el uso de:

- Javadoc
- Camel Case para variables y funciones
- UpperCamelCase para clases y records
- SCREAMING_SNAKE_CASE para constantes y variables de entorno
- Indentación estándar (2 espacios)
- Annotations

## Patrones de Diseño y Arquitectura

- **Arquitectura Hexagonal (Puertos y Adaptadores):**
    - El módulo `core` es un módulo de dominio en Java puro (sin frameworks, base de datos ni HTTP). Define el contrato
      de entrada a través de la interfaz `ICore`.
    - El módulo `controller` implementa los adaptadores de entrada (API REST, suscriptor MQTT) y salida (cliente REST de
      switches, persistencia MongoDB).

- **Inyección de Dependencias (IoC):**
    - Todo componente de Spring Boot (`@Service`, `@RestController`, `@Component`) debe recibir sus dependencias
      exclusivamente a través de **inyección por constructor** (evitar `@Autowired` sobre campos privados) para
      facilitar testeabilidad y bajo acoplamiento.

- **Singleton & Static Factory:**
    - El ciclo de vida de los servicios es Singleton gestionado por Spring.
    - La instanciación del motor del Core se desacopla mediante `CoreFactory.getCore()`, permitiendo exponerlo como
      `@Bean` en `CoreConfiguration`.

- **Event-Driven (Observer):**
    - Comunicación asíncrona y desacoplada mediante `ApplicationEventPublisher` (e.g. `UpdatedInventoryEvent`) para
      notificar cambios de configuración entre servicios sin acoplarlos directamente.

- **Inmutabilidad y DTOs:**
    - Uso preferente de Java `record` para DTOs, mensajes MQTT y objetos de valor (Value Objects), asegurando
      inmutabilidad en la transferencia de datos.

## Sistema de Lenguaje e Interpretación del Core

El Core opera como un **motor de decisión puro y desacoplado**, que no ejecuta acciones directamente ni conoce la
tecnología de los actuadores (REST, MQTT, etc.). Utiliza un esquema de **lenguaje de comandos / intérprete**:

1. **Entrada al Core (Snapshot del Estado):**
    - Recibe la hora de evaluación y un snapshot inmutable del sitio (`DataSite`), conteniendo el estado actual de las
      habitaciones y la configuración tarifaria.

2. **Lenguaje Intermedio de Decisión (DSL de Comandos):**
    - El Core no muta estado externo; procesa las reglas de negocio (tarifa punta, déficit de temperatura, balance de
      potencia) y genera como respuesta una secuencia de comandos abstractos (`CoreAnswer` que contiene objetos
      `ICommand` / `CommandSwitch`).
    - Cada comando describe la intención (*qué hacer*, e.g., `SwitchState.ENCENDIDO` o `SwitchState.APAGADO` sobre un
      `idSwitch`), abstrayéndose de *cómo* se ejecuta.

3. **Intérprete / Ejecutor (Engine):**
    - El componente orquestador (`TemperatureControlEngine`) actúa como el intérprete: lee la lista de comandos devuelta
      por el Core, contrasta con el estado real y despacha las peticiones HTTP correspondientes hacia la API de Switches
      (`SwitchClient`).
    - **Regla para desarrolladores:** Toda nueva regla o acción que decida el Core debe modelarse extendiendo `ICommand`
      en el módulo `core`, y su interpretación/ejecución debe implementarse en la capa del Engine (`controller`).



