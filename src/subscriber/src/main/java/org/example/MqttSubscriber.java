package org.example;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bson.Document;
import java.nio.charset.StandardCharsets;
public class MqttSubscriber {
    private static final AppConfig appConfig = AppConfig.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(MqttSubscriber.class);
    private static final String BROKER_URL = appConfig.getValues().get(appConfig.MQTT_BROKER_URL);
    private static final String CLIENT_ID = appConfig.getValues().get(appConfig.MQTT_CLIENT_ID);
    private static final String TOPIC = appConfig.getValues().get(appConfig.MQTT_TOPIC);
    private static final String MONGO_DB_URI = appConfig.getValues().get(appConfig.MONGO_DB_URI);
    private static final String MONGO_DB_DATABASE = appConfig.getValues().get(appConfig.MONGO_DB_DATABASE);
    private static final String MONGO_DB_COLLECTION = appConfig.getValues()
            .getOrDefault(appConfig.MONGO_DB_COLLECTION, "received_messages");
    private static final String ROOMS_API_URL = appConfig.getValues().get(appConfig.ROOMS_API_URL);
    private static final Controlador controlador = new Controlador(ROOMS_API_URL);

    private static final MqttSubscriber mqttSubscriber = new MqttSubscriber(BROKER_URL, CLIENT_ID, TOPIC);
    // Se puede usar un cliendId aleatorio como este por ejemplo.
    // private static final MqttSubscriber mqttSubscriber = new MqttSubscriber(BROKER_URL, MqttClient.generateClientId(), TOPIC);

    private MqttSubscriber(String broker, String clientId, String topic) {
        initiateConnection(broker, clientId, topic);
    }

    public static MqttSubscriber getInstance() {
        return mqttSubscriber;
    }

    private void initiateConnection(String broker, String clientId, String topic) {
        int qos = 1;

        try {
            MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());
            MongoMessageRepository repository = new MongoMessageRepository(
                    MONGO_DB_URI, MONGO_DB_DATABASE, MONGO_DB_COLLECTION);
            Runtime.getRuntime().addShutdownHook(new Thread(repository::close));

            // Set callback to handle incoming messages
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    logger.error("Connection lost: {}", cause.getMessage());
                }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            repository.save(topic, message);

            String payload =
                new String(message.getPayload(), StandardCharsets.UTF_8);

            Document json = Document.parse(payload);

            Temperatura temperatura = new Temperatura(
                json.getInteger("id"),
                json.getDouble("tC"),
                json.getDouble("tF"),
                json.getDouble("ts")
        );

            Orden orden = controlador.controlar(temperatura);

            logger.info(
                "Termostato {} -> orden {}",
                temperatura.id(),
                orden
        );

        } catch (RuntimeException exception) {
            logger.error(
                "Could not process MQTT message from topic {}",
                topic,
                exception
            );
        }
    }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Unused for subscribers
                }
            });

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            logger.info("Connecting to broker: {}", broker);
            client.connect(connOpts);
            logger.info("Connected!");

            // Subscribe to the topic
            client.subscribe(topic, qos);
            logger.info("Subscribed to topic: {}", topic);

        } catch (MqttException me) {
            logger.error("reason {}", me.getReasonCode());
            logger.error("msg {}", me.getMessage());
            logger.error("loc {}", me.getLocalizedMessage());
            logger.error("cause {}", me.getCause());
            me.printStackTrace();
        }
    }
}
