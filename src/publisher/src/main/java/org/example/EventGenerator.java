package org.example;

import jakarta.inject.Singleton;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Singleton
public class EventGenerator {
    private final float startingTemp = 24.0f;
    private final float deltaTemp = 0.3f;
    private final float absoluteMinTemp = -2.0f;
    private final float absoluteMaxTemp = 37.0f;

    private final int delayMillis = 500;
//    private final Float deltaTemp = 0.3f;

    private final Random random = new Random();
    private final PublisherFactory publisherFactory;

    public EventGenerator(PublisherFactory publisherFactory){
        this.publisherFactory = publisherFactory;
        this.generate();
    }

    public void generate(){
        EventPublisher publisher = this.publisherFactory.buildPublisher("home/sensors/temperature");

        // Una solucion de paralelismo
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 2; i++) {
                int finalI = i;
                executor.submit(() -> this.spamEventMessage(publisher, finalI));
            }
        }
    }

    private void spamEventMessage(EventPublisher publisher, int id) {
        float lastTemp = this.startingTemp;

        try {
            for (int i = 0; i < 10; i++) {
                float min = Math.max(this.absoluteMinTemp, lastTemp - this.deltaTemp);
                float max = Math.min(this.absoluteMaxTemp, lastTemp + this.deltaTemp);
                lastTemp = this.random.nextFloat(min, max);
                // TODO: Podria forzar tendencias sostenidas de elevacion / reduccion de la temperatura para formar cambios mas amplios

                double epochTime = java.time.Instant.now().toEpochMilli() / 1000.0;
                String message = "{\"id\":" + id + ",\"tC\":" + "%.2f".formatted(lastTemp) + ",\"tF\":0.0,\"ts\":" + "%.3f".formatted(epochTime) + "}";
                publisher.publish(message);

                // Randomiza el delay entre eventos para darle algo de inconsistencia e imitar deltas no exactos
                int delay = this.random.nextInt(delayMillis - 25, delayMillis + 25);
                TimeUnit.MILLISECONDS.sleep(delay);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
