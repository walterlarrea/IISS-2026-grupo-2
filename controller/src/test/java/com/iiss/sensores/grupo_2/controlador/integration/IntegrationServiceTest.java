package com.iiss.sensores.grupo_2.controlador.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class IntegrationServiceTest {

  @Test
  void registersFetchesInventoryAndConnectsToReturnedBrokerAndDeclaredTopics() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    MqttSubscriptionService mqttSubscriptionService = mock(MqttSubscriptionService.class);

    server
        .expect(requestTo("http://iiss-simulator:9090/controladores"))
        .andExpect(method(POST))
        .andExpect(header("X-API-Key", "simulator-api-key"))
        .andExpect(
            content()
                .json(
                    """
                        {
                          "etiqueta":"control-casa",
                          "baseUrl":"http://controlador:8080",
                          "apiKey":"controller-api-key"
                        }
                        """))
        .andRespond(
            withSuccess(
                """
                        {
                          "id":"control-casa",
                          "etiqueta":"control-casa",
                          "baseUrl":"http://controlador:8080",
                          "registradoEn":"2026-10-12T20:00:00Z",
                          "mqtt":{"host":"broker.test","puerto":1883}
                        }
                        """,
                MediaType.APPLICATION_JSON));

    IntegrationService service =
        new IntegrationService(
            builder,
            mqttSubscriptionService,
            true,
            "http://iiss-simulator:9090",
            "simulator-api-key",
            "control-casa",
            "http://controlador:8080",
            "controller-api-key",
            "tcp://localhost:1883",
            "controlador-test");

    service.onApplicationReady();

    verify(mqttSubscriptionService).conectar("tcp://broker.test:1883", "controlador-test");
    server.verify();
  }
}
