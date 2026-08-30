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
    private static final Logger logger = LoggerFactory.getLogger(EventGenerator.class);

    // Temperatura inicial de todos los publicadores
    private final float startingTemp = 24.0f;

    // Diferencia maxima (- y +) del cambio de temperatura entre eventos
    private final float deltaTemp = 0.3f;

    // Minimo y maximo absoluto al que puede llegar la temperatura
    private final float absoluteMinTemp = -2.0f;
    private final float absoluteMaxTemp = 37.0f;

    // Tiempo de espera base entre eventos
    private final int delayMillis = 500;
    private final int delayDeltaMillis = 25;

    private final Random random = new Random();
    private final PublisherFactory publisherFactory;

    @Inject
    public EventGenerator(PublisherFactory publisherFactory){
        this.publisherFactory = publisherFactory;
    }

    public void generate(){
        ArrayList<String> topics = new ArrayList<String>(List.of("ht-sim-room1/status/temperature:0", "ht-sim-room2/status/temperature:0", "ht-sim-room3/status/temperature:0"));
        int id = 0;

        // Una solucion de paralelismo
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String topic: topics) {
                EventPublisher publisher = this.publisherFactory.buildPublisher(topic);
                int finalId = id;
                executor.submit(() -> this.spamEventMessage(publisher, finalId));
                id++;
            }
        }catch(Exception e){
            logger.error("Error occur while running publishers");
            logger.error(String.valueOf(e));
        }
    }

    private void spamEventMessage(EventPublisher publisher, int id) {
        logger.info("Starting event publisher with ID = {}", id);

        float lastTemp = this.startingTemp;

        while (true) {
            try {
                float min = Math.max(this.absoluteMinTemp, lastTemp - this.deltaTemp);
                float max = Math.min(this.absoluteMaxTemp, lastTemp + this.deltaTemp);

                // Randomiza el siguiente valor de temperatura a publicar. Con tope minimo y maximo
                lastTemp = this.random.nextFloat(min, max);

                // TODO: Podria forzar tendencias sostenidas de elevacion / reduccion de la temperatura para formar cambios mas amplios

                // Toma el tiempo epoch actual y lo convierte a segundos
                float epochTime = (java.time.Instant.now().toEpochMilli() / 1000.0f);

                // Crea el JSON
                Temperatura temp = new Temperatura(id, lastTemp, 0.0f, epochTime);
                String message = MessageObject.validateJson(temp);

                // Envia el mensaje
                publisher.publish(message);

                int minDelay = this.delayMillis - this.delayDeltaMillis;
                int maxDelay = this.delayMillis + this.delayDeltaMillis;

                // Randomiza el delay entre eventos para darle algo de inconsistencia y que no tenga un tiempo preciso
                int delay = this.random.nextInt(minDelay, maxDelay);
                TimeUnit.MILLISECONDS.sleep(delay);
            }catch(InterruptedException e){
                logger.error("InterruptedException");
                logger.error(String.valueOf(e));
                break;
            } catch (Exception e) {
                logger.error("Exception");
                logger.error(String.valueOf(e));
                throw new RuntimeException(e);
            }
        }
    }
}
