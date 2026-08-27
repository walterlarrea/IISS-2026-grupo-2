package org.example;

import io.avaje.inject.BeanScope;

import java.util.concurrent.CountDownLatch;

public class Main {
    private static EventGenerator eventGenerator;

    public static void main(String[] args) throws InterruptedException {
        try (BeanScope scope = BeanScope.builder().build()) {
            EventGenerator eventGenerator = scope.get(EventGenerator.class);

            eventGenerator.generate();
        } // 3. The scope closes automatically here

        new CountDownLatch(1).await();
    }
}
