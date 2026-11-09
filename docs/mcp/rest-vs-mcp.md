# Comparación de Diseño de Contratos: API REST vs API MCP

Este documento analiza las diferencias conceptuales, de diseño y de consumo entre el **API REST** del controlador (basado en recursos, verbos HTTP y especificación OpenAPI) y el **API MCP** (Model Context Protocol, basado en herramientas, nombres descriptivos y semántica en lenguaje natural).

---

## 1. Tabla de Mapeo de Operaciones

| Operación del Sistema | API REST (Recurso + Verbo) | API MCP (Tool + Parámetros) | Justificación del Cambio de Contrato |
| :--- | :--- | :--- | :--- |
| **Listar configuración de habitaciones** | `GET /habitaciones` | `listar_habitaciones` *(sin params)* | En REST, el cliente navega a la colección mediante `GET`. En MCP, el modelo decide consultar el inventario para aprender IDs y dispositivos disponibles. |
| **Consultar detalle de habitación** | `GET /habitaciones/{id}` | `obtener_habitacion(id)` | El parámetro de path de REST se traduce a un argumento tipado con descripción que indica al LLM cómo deducir o buscar el `id`. |
| **Cambiar temperatura deseada (setpoint)** | `PATCH /habitaciones/{id}` *(cuerpo JSON con mapa de campos)* | `modificar_temperatura_esperada(id, temperaturaEsperada)` | REST usa mutación parcial genérica sobre el recurso. En MCP se expone una herramienta de intención específica con unidades explícitas (Celsius) para evitar que el LLM arme payloads incorrectos. |
| **Crear nueva habitación** | `POST /habitaciones` *(cuerpo JSON `Habitacion`)* | `crear_habitacion(id, nombre, temperaturaEsperada, potenciaKW, idTermostato, topicTermostato, idSwitch, urlSwitch)` | El esquema OpenAPI documenta tipos estáticos; el esquema MCP describe además restricciones de unicidad y significado físico de cada atributo. |
| **Activar switch de habitación** | `POST /switches/{idSwitch}/comandos` con `{"accion":"ON"}` | `activar_switch(idHabitacion)` | **Abstracción semántica:** El LLM no necesita conocer la topología de red ni la URL del switch: pide encender el ambiente y el servidor MCP orquesta la resolución de URLs y llamadas downstream. |
| **Desactivar switch de habitación** | `POST /switches/{idSwitch}/comandos` con `{"accion":"OFF"}` | `desactivar_switch(idHabitacion)` | Herramienta simétrica de intención directa. |
| **Consultar estado del switch** | `GET /switches/{idSwitch}` | `consultar_estado_switch(idHabitacion)` | Permite al modelo comprobar si el dispositivo ya está accionado antes de emitir un comando. |
| **Consultar estado del controlador** | `GET /control` | `obtener_estado_controlador` *(sin params)* | Retorna si el motor está `EN_EJECUCION` o `DETENIDO`, la hora sintética de simulación y el factor de aceleración. |
| **Iniciar motor de control** | `POST /control/comandos` con `{"accion":"INICIAR", ...}` | `iniciar_controlador(fechaHora, factorTiempo)` | Convierte un comando polimórfico REST con discriminador en una función invocable con parámetros descritos en formato ISO-8601. |
| **Detener motor de control** | `POST /control/comandos` con `{"accion":"DETENER"}` | `detener_controlador` *(sin params)* | Acción atómica sin payload superfluo. |

---

## 2. Diferencias Conceptuales: Programa vs Modelo de Lenguaje (LLM)

### 2.1 El Consumidor del Contrato

* **En REST (Consumidor: Código / Programa):**
  * El desarrollador lee la documentación (OpenAPI / Swagger) en tiempo de diseño y escribe código cliente determinista (`HttpClient`, `fetch`).
  * Los errores de esquema se detectan en tiempo de compilación o deserialización.
  * Los códigos de estado HTTP (`200`, `204`, `400`, `404`, `401`) controlan el flujo de control del programa.

* **En MCP (Consumidor: Modelo de Inteligencia Artificial):**
  * El LLM inspecciona la lista de *tools* en **tiempo de ejecución** dentro de su ciclo de razonamiento (ReAct / Tool Calling).
  * **La descripción es parte activa del contrato ejecutable:** el modelo compara la intención del usuario en lenguaje natural con las descripciones de las tools para decidir cuál llamar, en qué orden y con qué argumentos.
  * Si la descripción es ambigua, el modelo puede alucinar parámetros, ignorar la herramienta o elegir una errónea.

### 2.2 Granularidad y Semántica

1. **Intención vs Estado del Recurso:**
   * En REST es idiomático modelar recursos sustantivos y mutar su estado (`PATCH /habitaciones/1` con `{"temperaturaEsperada": 20}`).
   * En MCP es preferible modelar **intenciones de usuario** (`modificar_temperatura_esperada`). Un LLM comprende mejor verbos directos orientados a tareas que construir parches de objetos JSON complejos.
2. **Ocultamiento de Topología e Infraestructura:**
   * El API REST exige que quien opere switches conozca el `idSwitch` y la `urlSwitch`.
   * El API MCP asume un rol de asistente de domótica: el usuario pide *"encendé el living"*, por lo que la tool recibe `idHabitacion` y el adaptador delgado se encarga de resolver las coordenadas del switch invocando internamente al controlador y luego al microservicio de switches.
3. **Manejo de Errores y Retorno:**
   * En REST, un `404 Not Found` o un `400 Bad Request` corta la ejecución con una excepción HTTP.
   * En MCP, el servidor debe retornar mensajes descriptivos en formato texto/JSON estructurado para que el modelo pueda autocorregirse (e.g., si un ID no existe, el LLM puede invocar `listar_habitaciones` para recuperarlo).
