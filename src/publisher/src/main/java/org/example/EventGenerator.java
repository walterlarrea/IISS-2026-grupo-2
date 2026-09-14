package org.example;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.example.dto.MessageObject.MessageObject.Temperatura;
import org.example.dto.MessageObject.MessageObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Singleton
public class EventGenerator {

    private static final Logger logger =
            LoggerFactory.getLogger(EventGenerator.class);

    // Temperatura inicial de todos los publicadores
    private final float startingTemp = 24.0f;

    // Diferencia máxima del cambio de temperatura entre eventos
    private final float deltaTemp = 0.3f;

    // Mínimo y máximo absoluto de temperatura
    private final float absoluteMinTemp = -2.0f;
    private final float absoluteMaxTemp = 37.0f;

    // Tiempo aproximado entre eventos
    private final int delayMillis = 3000;
    private final int delayDeltaMillis = 250;

    private final Random random = new Random();

    private final PublisherFactory publisherFactory;

    @Inject
    public EventGenerator(PublisherFactory publisherFactory) {
        this.publisherFactory = publisherFactory;
    }

    public void generate() {

        ArrayList<String> topics = new ArrayList<>(
                List.of(
                        "ht-sim-room1/status/temperature:0",
                        "ht-sim-room2/status/temperature:1"
                )
        );

        int id = 0;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (String topic : topics) {

                EventPublisher publisher =
                        this.publisherFactory.buildPublisher(topic);

                int finalId = id;

                executor.submit(() ->
                        this.spamEventMessage(publisher, finalId)
                );

                id++;
            }

        } catch (Exception e) {
            logger.error("Error occurred while running publishers", e);
        }
    }

    private void spamEventMessage(EventPublisher publisher, int id) {

        logger.info("Starting event publisher with ID = {}", id);

        float lastTemp = this.startingTemp;

        while (true) {

            try {

                float min = Math.max(
                        this.absoluteMinTemp,
                        lastTemp - this.deltaTemp
                );

                float max = Math.min(
                        this.absoluteMaxTemp,
                        lastTemp + this.deltaTemp
                );

                // Genera una nueva temperatura
                lastTemp = this.random.nextFloat(min, max);

                // Tiempo actual en segundos desde Epoch
                float epochTime =
                        java.time.Instant.now().toEpochMilli() / 1000.0f;

                // Crea la temperatura
                Temperatura temp =
                        new Temperatura(
                                id,
                                lastTemp,
                                0.0f,
                                epochTime
                        );

                // Convierte a JSON
                String message =
                        MessageObject.validateJson(temp);

                // Publica el mensaje
                publisher.publish(message);

                logger.info(
                        "Published temperature: id={}, tC={}",
                        id,
                        lastTemp
                );

                // Espera entre eventos
                int minDelay =
                        this.delayMillis - this.delayDeltaMillis;

                int maxDelay =
                        this.delayMillis + this.delayDeltaMillis;

                int delay =
                        this.random.nextInt(minDelay, maxDelay);

                TimeUnit.MILLISECONDS.sleep(delay);

            } catch (InterruptedException e) {

                logger.error("Event publisher interrupted", e);
                break;

            } catch (Exception e) {

                logger.error("Error publishing event", e);
                throw new RuntimeException(e);
            }
        }
    }
}