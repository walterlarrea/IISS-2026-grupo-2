## Escenarios del Producto: IoTEste EcoWarm

### Escenario 1: Optimización Automática por Tarifa Multihorario y Pronóstico del Tiempo

_(Orientado al hogar / usuario con perfil “Persona normal / analfabeto digital”)_

- **Contexto y Situación:**
  La familia Rodríguez vive en una casa con 4 habitaciones equipadas con losa radiante (cada una con su termostato Shelly H&T y un switch de control). Tienen contratada la **tarifa residencial multihorario de UTE**, donde el costo de la energía varía considerablemente según la hora del día (tramo Pico de alto costo vs. tramos Valle/Llano de menor costo). La familia no quiere estar pendiente de encender o apagar la calefacción manualmente ni de calcular horarios; buscan confort continuo sin sorpresas en la factura eléctrica.
- **Funcionamiento Diario:**
  EcoWarm analiza continuamente la **temperatura objetivo** configurada para cada habitación (por ejemplo, 21 °C en el living y 19 °C en los dormitorios). A través de su _Controlador_, el sistema consulta el **pronóstico del tiempo para las próximas 48 horas** y cruza estos datos con el esquema de la **tarifa multihorario de UTE**. Sabedor de que la losa radiante es un sistema con alta inercia térmica (tarda horas en calentar y en enfriarse), el algoritmo evalúa la inercia registrada previamente en esas habitaciones para tomar decisiones:
- **Antes de entrar al tramo Pico de UTE:** EcoWarm aprovecha la tarifa más económica del tramo Valle/Llano para realizar un precalentamiento estratégico en las habitaciones prioritarias.
- **Durante el tramo Pico de UTE:** EcoWarm apaga o modula los switches de la losa radiante en ciertas habitaciones. Gracias al calor acumulado previamente, la temperatura interior se mantiene dentro de los márgenes de confort deseados sin requerir consumo eléctrico de alto costo.

<!-- PROPUESTAS A FUTURO
- **Respuesta a variables externas:** Si los sensores de temperatura/humedad exteriores registran una helada imprevista antes de lo pronosticado, EcoWarm reajusta dinámicamente la ventana de calentamiento previo.

- **Resultados e Interacción:**
  Al final del mes, la familia logra mantener el confort térmico en todo el hogar evitando el consumo de energía en los bloques tarifarios más caros. A través de un bot asistido por **GenAI (LLM)**, el usuario consulta mediante un mensaje en lenguaje natural: _"¿Cómo se comportó la calefacción esta semana?"_, a lo que el sistema responde de forma sencilla: _"Precalentamos la casa a las 16:00 hs antes del horario Pico de UTE. Evitamos el uso de losa radiante durante el tramo caro y ahorraste un 22% de energía respecto a la semana anterior manteniendo los 21 °C objetivo"_.
  -->

---

### Escenario 2: Análisis de Inercia Térmica y Simulación para Ajuste de Potencia Contratada

_(Orientado al perfil “Técnico especialista” / Administración de espacios como oficinas o empresas tipo CallCenter)_

- **Contexto y Situación:**
  _CallSolutions_ es una empresa que ocupa un piso de oficinas con 6 zonas independientes controladas por losa radiante. El especialista en mantenimiento térmico de la empresa necesita evaluar la eficiencia del sistema y ajustar la potencia contratada con UTE, ya que sospecha que están pagando de más por exceso de potencia o corriendo el riesgo de que salte la llave térmica al encender todas las habitaciones en simultáneo durante el invierno.
- **Funcionamiento Diario:**
  Durante varias semanas, el _Controlador_ de EcoWarm persiste minuto a minuto los eventos MQTT que llegan desde los termostatos (Shelly H&T) y registra el estado de encendido/apagado de los switches de cada oficina.

1. **Recopilación de Históricos:** El sistema procesa los datos recopilados para calcular exactamente cuánto tiempo le toma a cada oficina subir 1 °C (_velocidad de calentamiento_) y cuánto tarda en perder 1 °C (_velocidad de enfriamiento_) según la temperatura exterior.
2. **Ejecución de Simulaciones:** El especialista ingresa al módulo de simulación de EcoWarm e introduce diferentes escenarios hipotéticos: _"¿Qué sucede si reducimos la potencia contratada de 15 kW a 10 kW?"_ o _"¿Cuántas zonas pueden permanecer encendidas en simultáneo sin superar el límite establecido?"_.

- **Resultados e Interacción:**
  El simulador muestra que, debido a la excelente retención térmica de las oficinas interiores, no es necesario encender las 6 zonas al mismo tiempo. El sistema le sugiere al técnico un esquema de **turnos rotativos de encendido/apagado** entre zonas para mitigar los picos de demanda eléctrica. El técnico especialista aprueba la nueva planificación recomendada por el sistema, logrando reducir la potencia contratada de la empresa ante UTE sin sacrificar el confort de los empleados.

---

### Escenario 3: Control Inteligente con GenAI y Gestión de Horarios Variables sobre la Marcha

_(Orientado al perfil “Entusiasta de la domótica” / Usuario con horarios dinámicos)_

> Atención:
> El sistema de «GenAI» mencionado más adelante no forma parte del producto EcoWarm,
> sino que refiere a agentes de Inteligencia Artificial Generativa de terceros a los que el usuario haya otorgado acceso a su sistema EcoWarm.

- **Contexto y Situación:**
  Martín es un profesional independiente que tiene horarios de trabajo muy impredecibles. Algunas tardes regresa a su casa a las 17:00 hs y otras a las 22:00 hs. Tiene configurado el sistema EcoWarm en su hogar con 3 habitaciones controladas. Le gusta integrar sus dispositivos inteligentes y quiere ajustar el clima de su casa de manera remota mediante interacción natural (vía voz o chat, por ejemplo, integrada con asistentes o sistemas como Android Auto / Apple CarPlay).
- **Funcionamiento Diario:**
  Camino a su casa en auto tras una reunión que terminó antes de lo previsto, Martín activa el asistente por voz y dice: _"Dile a EcoWarm que voy a llegar a casa en 45 minutos y quiero el dormitorio principal y la sala de estar a 21 °C"_.
- **Procesamiento por GenAI (LLM):** El modelo de lenguaje interpreta la intención del usuario (_intención: precalentar; objetivo: 21 °C; tiempo de llegada: 45 minutos; zonas: Dormitorio Principal, Sala_).
- **Evaluación de factibilidad y tarifas:** EcoWarm consulta la temperatura actual enviada por los sensores MQTT de esas habitaciones, revisa la inercia térmica registrada (sabe que la losa radiante del dormitorio tarda 30 minutos en subir 2 °C) y verifica el esquema tarifario vigente de UTE.
- **Acción:** El _Controlador_ determina que encender la sala de inmediato cumplirá con la temperatura a tiempo y que el dormitorio solo necesita 20 minutos de encendido previo. Genera la orden para activar los switches correspondientes de forma escalonada.

- **Resultados e Interacción:**
  El sistema de GenAI (LLM) responde por voz/texto (Ejemplo): _"Entendido. Ya encendí la losa de la sala de estar y programé el dormitorio para encenderse en 20 minutos. Llegarás con ambas habitaciones a 21 °C con un gasto eléctrico optimizado dentro de tu tarifa de UTE"_. Al llegar a la casa, el ambiente está a la temperatura deseada sin haber dejado la calefacción encendida todo el día.
