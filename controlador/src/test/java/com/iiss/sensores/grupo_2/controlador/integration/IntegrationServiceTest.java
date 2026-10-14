package com.iiss.sensores.grupo_2.controlador.integration;

import com.iiss.sensores.grupo_2.controlador.api.service.InventarioService;
import com.iiss.sensores.grupo_2.controlador.engine.mqtt.MqttSubscriber;
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
        InventarioService inventoryService = mock(InventarioService.class);
        MqttSubscriber mqttSubscriber = mock(MqttSubscriber.class);

        server.expect(requestTo("http://iiss-simulator:9090/controladores"))
                .andExpect(method(POST))
                .andExpect(header("X-API-Key", "simulator-api-key"))
                .andExpect(content().json("""
                        {
                          "etiqueta":"control-casa",
                          "baseUrl":"http://controlador:8080",
                          "apiKey":"controller-api-key"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "id":"control-casa",
                          "etiqueta":"control-casa",
                          "baseUrl":"http://controlador:8080",
                          "registradoEn":"2026-10-12T20:00:00Z",
                          "mqtt":{"host":"broker.test","puerto":1883}
                        }
                        """, MediaType.APPLICATION_JSON));
        // server.expect(requestTo("http://iiss-simulator:9090/sitio"))
        //         .andExpect(method(GET))
        //         .andRespond(withSuccess("""
        //                 {
        //                   "schemaVersion":"2.0",
        //                   "sitio":{
        //                     "id":"casa",
        //                     "nombre":"Casa",
        //                     "potenciaContratadaKW":3.7,
        //                     "tarifa":{"punta":{"desde":"17:00","hasta":"23:00","dias":"HABILES"}}
        //                   },
        //                   "habitaciones":[{
        //                     "id":"room1",
        //                     "nombre":"Living",
        //                     "temperaturaEsperada":21.5,
        //                     "potenciaKW":1.2,
        //                     "idTermostato":"ht-sim-room1",
        //                     "topicTermostato":"ht-sim-room1/status/temperature:0",
        //                     "idSwitch":"sw-sim-room1",
        //                     "urlSwitch":"http://iiss-simulator:9090"
        //                   }]
        //                 }
        //                 """, MediaType.APPLICATION_JSON));

        IntegrationService service = new IntegrationService(
                builder,
                inventoryService,
                mqttSubscriber,
                true,
                "http://iiss-simulator:9090",
                "simulator-api-key",
                "control-casa",
                "http://controlador:8080",
                "controller-api-key",
                "tcp://localhost:1883",
                "controlador-test",
                "#"
        );

        service.onApplicationReady();

        verify(mqttSubscriber).conectar(
                "tcp://broker.test:1883",
                "controlador-test",
                "#"
        );
        server.verify();
    }
}
