package org.example;

import java.util.concurrent.CountDownLatch;

public class Main {
    static void main() throws InterruptedException {
        IO.println(String.format("Hello and welcome!"));

        for (int i = 1; i <= 5; i++) {
            IO.println("i = " + i);
        }

        new CountDownLatch(1).await();
    }
}
