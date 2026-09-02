# 1. Metodología Agil elegida
### Alternativas:
* Scrum: Destaca por sus iteraciones estructuradas (sprints), roles definidos y ceremonias estrictas. Sin embargo, puede volverse demasiado rígido para la gestión dinámica del día a día en un equipo técnico enfocado en programación y despliegue rápido.
* Kanban: Se enfoca totalmente en el flujo continuo de trabajo y la visualización de cuellos de botella a través de un tablero, pero carece de los ciclos temporales fijos (timeboxing) necesarios para cumplir con los cierres de alcance estipulados en cada iteración del curso.
#### Por qué elegimos Scrumban:
* Incorpora un flujo de trabajo continuo y transparente a través de columnas en el tablero (como se observa en la gestión visual con estados como To Do, In Progress, PR Review, Testing y Done), facilitando que el equipo colabore de forma sincronizada en los distintos componentes (como Java, persistencia y contenedores) sin la rigidez de los roles estrictos de Scrum.
* Permite trabajar bajo el concepto de sprints o ciclos de tiempo acotado requeridos por la materia, asegurando entregables funcionales en fechas específicas.
* Junta las mejores cualidades de las dos metodologías, ayudando a mejorar la organización y repartición de tareas para facilitar el desarrollo del equipo.
# 2. Tecnologías y Herramientas Utilizadas
### Java: 
* Lenguaje principal del proyecto. Elegido por su tipado fuerte y ecosistema robusto, lo cual facilita la integración del código desarrollado en paralelo por los distintos integrantes del equipo, reduciendo errores en comparación con lenguajes dinámicos como Python o Node.js.
### Apache Maven:
* Gestor de dependencias y ciclo de vida. Frente a opciones como Gradle (más complejo) o Ant (manual), Maven nos brinda un estándar declarativo claro (pom.xml) que automatiza las descargas, la ejecución de pruebas y facilita la integración continua.
### Persistencia y Base de DatosMongoDB:
* Base de datos NoSQL orientada a documentos. Se descartaron bases relacionales (como MySQL o PostgreSQL) porque los datos de telemetría de los sensores de las habitaciones pueden cambiar de estructura, y MongoDB nos da la flexibilidad necesaria sin requerir migraciones complejas de esquema. 
### Mensajería IoTMQTT (Broker Mosquitto):
* Protocolo de comunicación ligero. Se eligió por sobre HTTP/REST porque MQTT funciona bajo un modelo publicador/suscriptor de baja latencia, ideal para los eventos de temperatura, ahorrando ancho de banda.
### Eclipse Paho:
* Cliente MQTT para Java. Es el estándar de la industria, maduro y estable, lo que evita que tengamos que programar la gestión de sockets TCP desde cero o lidiar con reconexiones manuales.
### Despliegue y ContenedoresDocker y Docker Compose:
* Permiten empaquetar la aplicación y sus dependencias en contenedores ligeros. Se descartaron las Máquinas Virtuales por ser demasiado pesadas. Docker Compose nos garantiza que el build sea completamente reproducible sin depender de herramientas instaladas localmente en cada una de nuestras máquinas.
### Uso de Logs, logger en vez de println
* System.out.println imprime todo de forma plana sin distinción. Un Logger permite categorizar los mensajes por niveles (DEBUG, INFO, WARN, ERROR), permitiendo silenciar los datos de depuración en producción y capturar solo lo importante.
* Los logs gestionan de manera segura el acceso simultáneo de múltiples hilos evitando que la información se mezcle o corrompa en la salida. Esta es la principal razón por la que nos beneficia al manejar mensajes de tipo JSON y su persistencia.
## Utilidades Adicionales
### SLF4J:
* Fachada para el registro de logs. Estandariza la salida de consola, permitiendo niveles de severidad (INFO, ERROR), lo cual es infinitamente superior a usar simples System.out.println para depurar el sistema entre todos.
### JUnit 5:
* Framework estándar de pruebas unitarias en Java, elegido por su perfecta integración con el ciclo de Maven.
### Maven Shade Plugin:
* Herramienta clave que empaqueta nuestro código y todas las librerías en un único archivo ejecutable (Uber-JAR), simplificando enormemente la ejecución dentro del contenedor Docker.




