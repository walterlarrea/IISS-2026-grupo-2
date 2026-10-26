package com.iiss.sensores.grupo_2.controlador.integration;

import com.iiss.sensores.grupo_2.controlador.engine.persistence.TemperatureReadingRepository;
import com.iiss.sensores.grupo_2.dto.TemperatureMessage;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class TemperatureReadingRepositoryIT {
    private static final int MONGO_PORT = 27017;

    @Container
    private static final GenericContainer<?> MONGODB =
            new GenericContainer<>("mongo:8")
                    .withExposedPorts(MONGO_PORT)
                    .waitingFor(Wait.forListeningPort());

    @Test
    void savesAndReturnsLatestTemperatureForEachThermostat() {
        // Comprueba con MongoDB real que se filtra por idThermostat y se devuelve la lectura más reciente.
        String databaseName = "integration_" + UUID.randomUUID().toString().replace("-", "");
        String collectionName = "readings";
        String connectionString =
                "mongodb://" + MONGODB.getHost() + ":" + MONGODB.getMappedPort(MONGO_PORT);

        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            TemperatureReadingRepository repository =
                    new TemperatureReadingRepository(mongoClient, databaseName, collectionName);
            Instant firstReading = Instant.parse("2026-10-24T10:00:00Z");
            Instant latestReading = Instant.parse("2026-10-24T10:05:00Z");

            repository.saveTemperature(new TemperatureMessage(
                    "reading-1", "thermostat-1", 19.0, 66.2, firstReading));
            repository.saveTemperature(new TemperatureMessage(
                    "reading-2", "thermostat-1", 21.5, 70.7, latestReading));
            repository.saveTemperature(new TemperatureMessage(
                    "reading-3", "thermostat-2", 18.0, 64.4, firstReading));

            List<TemperatureMessage> latestReadings =
                    repository.getLastTemperatureMeasurements(
                            List.of("thermostat-1", "thermostat-2", "unknown-thermostat"));

            assertEquals(2, latestReadings.size());
            assertEquals(
                    new TemperatureMessage("reading-2", "thermostat-1", 21.5, 70.7, latestReading),
                    latestReadings.stream()
                            .filter(reading -> "thermostat-1".equals(reading.idThermostat()))
                            .findFirst()
                            .orElseThrow());
            assertEquals(
                    new TemperatureMessage("reading-3", "thermostat-2", 18.0, 64.4, firstReading),
                    latestReadings.stream()
                            .filter(reading -> "thermostat-2".equals(reading.idThermostat()))
                            .findFirst()
                            .orElseThrow());
        } finally {
            try (MongoClient mongoClient = MongoClients.create(connectionString)) {
                mongoClient.getDatabase(databaseName).drop();
            }
        }
    }
}
