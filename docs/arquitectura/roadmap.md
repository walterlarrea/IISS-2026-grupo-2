# Limitaciones conocidas y Roadmap — EcoWarm

Estado al cierre de la **Iteración 6**. Este documento está pensado para el equipo que continúa el
desarrollo: describe qué no hace el controlador hoy, qué problemas conocidos tiene el código y por dónde seguir.

Las decisiones que originan varias de estas limitaciones están explicadas en [decisiones.md](decisiones.md).

---

## 1. Limitaciones funcionales

| # | Limitación | Impacto |
|---|---|---|
| L1 | No usa temperatura exterior ni pronóstico meteorológico. | No anticipa olas de frío ni precalienta antes de que empeore el clima. |
| L2 | El control es reactivo: decide con la última lectura, sin modelo predictivo. | Con losa radiante (alta inercia) puede pasarse o quedarse corto de la temperatura objetivo. |
| L3 | La banda de inercia térmica (`CONTROLADOR_INERCIA_TERMICA_C`) y la histéresis (`CONTROLADOR_HISTERESIS_ENCENDIDO_C`) son globales. | No representan las características de cada habitación (tamaño, aislamiento, potencia). |
| L4 | La tarifa punta solo apaga todo; no hay precalentamiento antes del tramo punta. | Las habitaciones se enfrían durante la punta. |
| L5 | Una sola temperatura objetivo por habitación, sin agenda por horario o día. | No se puede bajar la temperatura de noche o cuando la casa está vacía. |
| L6 | Un controlador maneja un único sitio. | Para varios sitios hay que levantar varias instancias. |

## 2. Limitaciones técnicas y problemas conocidos

| # | Problema | Dónde | Sugerencia |
|---|---|---|---|
| P1 | Los mensajes MQTT publicados mientras el broker o el controlador están caídos se pierden ('cleanSession = true', QoS 1). | `MqttSubscriber` | Aceptable porque la siguiente lectura reemplaza a la anterior; si hiciera falta historial completo, usar sesión persistente. |
| P2 | La API de habitaciones ('RoomRepository', colección 'habitaciones') es heredada de iteraciones anteriores; el control usa el inventario del sitio ('PUT /sitio', colección 'inventario_controlador'). | `RoomRepository` (`TODO: clean for iter 6`) | Unificar en el inventario o eliminar la API vieja. |
| P3 | Los errores de validación de 'PUT /sitio' no tienen un formato de respuesta definitivo. | `SiteController` (`TODO`) | Normalizar con `RestExceptionHandler`. |
| P4 | 'controlador.lectura-max-antiguedad-min' (15 min) no está expuesta en 'docker-compose.yml' ni en '.env.*'. | `SiteStateService` | Agregar `CONTROLADOR_LECTURA_MAX_ANTIGUEDAD_MIN` al compose. |
| P5 | El backoff de switches (30 s → 2 min) y el plazo para dejar de apagar habitaciones eliminadas (1 h) son constantes en el código. | `SwitchClient`, `SiteStateService` | Pasarlos a propiedades si el entorno real lo requiere. |
| P6 | La zona horaria ('America/Montevideo') está fija. | `CustomClock` | Hacerla configurable por sitio. |

## 3. Roadmap

### Deuda técnica
1. Resolver P2–P5: limpiar la API de habitaciones heredada, exponer las constantes como configuración.
3. Persistir el estado de runtime relevante (backoff, marcas de espera) para sobrevivir reinicios (L7).

### Funcionalidad
3. Agenda de encendido/apagado por habitación: horarios y temperaturas objetivo distintos por día de la semana (L5).
4. Precalentamiento antes del tramo punta aprovechando la inercia de la losa (L4).
5. Detección autónoma de la inercia de cada habitación a partir del historial de lecturas (L3).
6. Gráficas e historial de lecturas de temperatura y de encendidos.
7. Detección de fugas de calor (por ejemplo, una ventana abierta).
8. Integración con pronóstico meteorológico y temperatura exterior (L1).
9. Comparar consumo real con el estimado y recomendar ajustes de horarios o potencia (L8).
10. Ampliar el servidor MCP para que asistentes inteligentes consulten y configuren el sistema en lenguaje natural.
11. Soporte multi-sitio en una sola instancia (L6).
