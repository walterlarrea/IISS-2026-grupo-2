# Detén el controlador anterior con Ctrl+C antes de ejecutar esto.
export SPRING_PROFILES_ACTIVE=local
export CONECTAR_CON_SIMULADOR=true
export SIMULATOR_API_URL=http://127.0.0.1:9090
export SIMULATOR_API_KEY=
export REGISTRATION_RETRY_MS=5000
export CONTROLADOR_ETIQUETA=controller-local
export CONTROLADOR_API_URL=http://localhost:8080
export CONTROLADOR_API_KEY=development-key
export CONTROLADOR_API_PORT=8080
export MONGODB_URI=mongodb://localhost:27017
export MONGODB_DATABASE=sensores
export MQTT_CLIENT_ID=ControladorLocal

mvn -pl controller -am package -DskipTests
java -jar controller/target/controller-app.jar