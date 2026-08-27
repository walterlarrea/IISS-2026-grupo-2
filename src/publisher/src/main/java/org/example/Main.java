package org.example;

import io.avaje.inject.BeanScope;

import java.util.concurrent.CountDownLatch;

public class Main {
    private static EventGenerator eventGenerator;

    static void main() throws InterruptedException {
        IO.println(String.format("Hello and welcome!"));

        try (BeanScope scope = BeanScope.builder().build()) {
//            PublisherFactory publisherFactory = scope.get(PublisherFactory.class);
//
//            publisherFactory.buildPublisher("home/sensors/temperature");
        } // 3. The scope closes automatically here

        new CountDownLatch(1).await();
    }
}
