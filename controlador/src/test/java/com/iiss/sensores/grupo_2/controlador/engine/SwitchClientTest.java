package com.iiss.sensores.grupo_2.controlador.engine;

import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class SwitchClientTest {

    @Test
    void sendsSimulatorCommandWithApiKeyAndContractPayload() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SwitchClient client = new SwitchClient(builder.build(), "simulator-key");

        server.expect(requestTo("http://iiss-simulator:9090/switches/sw-sim-room1/comandos"))
                .andExpect(method(POST))
                .andExpect(header("X-API-Key", "simulator-key"))
                .andExpect(content().json("""
                        {"accion":"ON"}
                        """))
                .andRespond(withSuccess("""
                        {"idSwitch":"sw-sim-room1","estado":"ON","cambio":true,"aplicadoEn":"2026-10-12T20:00:00Z"}
                        """, MediaType.APPLICATION_JSON));

        client.encender("http://iiss-simulator:9090/switches/sw-sim-room1/comandos");

        server.verify();
    }
}
