# Visión del Producto — EcoWarm v2

**PARA** hogares y oficinas con calefacción por losa radiante

**QUE** buscan maximizar su confort y optimizar su consumo eléctrico.

**EL ECOWARM**

**ES** un componente de una solución de domótica para la gestión inteligente de la calefacción

**QUE** permite gestionar sensores y switches para controlar la temperatura de las habitaciones y optimizar el funcionamiento del sistema de calefacción.

**A DIFERENCIA DE** otras soluciones de domótica que automatizan acciones sin considerar el costo del consumo eléctrico

**NUESTRO PRODUCTO** realiza una gestión inteligente del consumo eléctrico, optimizando el funcionamiento de la calefacción de acuerdo con las tarifas disponibles en nuestro mercado.

# Ajustes

- Ajustarse al Simulador (v2 - puede evolucionar)
- Poder iniciar el sistema sin Publisher, Stub Switch ni MQTT Broker.
- ?? Unir Subscriber y Rooms-api como "Controller"
- Tests respecto al "Tiempo Virtual"
- ! IMPORTANTE: Tener en cuenta que los eventos llegan cada 5 minutos, no múltiples por minuto.
- Core es funcional, requiere un interprete.

# Iteraciones

- 4: Que funcione
- 5: Tiempo virtual (por factor x)
- 6: Que se defienda bien, anti fallos, etc

# Preguntas al PO

- Criterio de prioridad: 1 - Mayor déficit, 2 - Mayor tiempo sin encender, 3 - ??
  - Solo usar el criteria n1
- Si la de mayor déficit no se puede encender por falta de potencia, sigo probando con el resto de habitaciones ordenadas por déficit DESC?
