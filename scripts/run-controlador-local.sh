# Detén el controlador anterior con Ctrl+C antes de ejecutar esto.
export SPRING_PROFILES_ACTIVE=local
export SIMULATOR_API_URL=http://127.0.0.1:9090
export SIMULATOR_API_KEY=
export SIMULATOR_REGISTRATION_RETRY_MS=5000
export CONTROLADOR_ETIQUETA=controlador-local
export CONTROLADOR_BASE_URL=http://localhost:8080
export ROOMS_API_KEY=development-key
export CONTROLADOR_PORT=8080
export MONGODB_URI=mongodb://localhost:27017
export MONGODB_DATABASE=sensores
export MQTT_CLIENT_ID=ControladorLocal

mvn -pl controlador -am package -DskipTests
java -jar controlador/target/controlador-app.jar