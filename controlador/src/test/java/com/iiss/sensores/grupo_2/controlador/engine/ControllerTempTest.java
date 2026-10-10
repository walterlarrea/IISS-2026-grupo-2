package com.iiss.sensores.grupo_2.controlador.engine;

import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.engine.service.ControllerTempAuto;
import com.iiss.sensores.grupo_2.controlador.shared.RoomInfo;
import com.iiss.sensores.grupo_2.controlador.shared.RoomProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ControllerTempTest {
    private SwitchClient switchClient;
    private RoomProvider roomProvider;
    private ControllerTempAuto controller;

    @BeforeEach
    void setUp() {
        switchClient = mock(SwitchClient.class);
        roomProvider = mock(RoomProvider.class);
        controller = new ControllerTempAuto(switchClient, roomProvider);
    }

    @Test
    void turnOnSwitchIfBelowExpectedTemp() throws Exception {
        controller.iniciar();
        controller.controlarTemp(18.0, 20.0, "http://switches-api:8090/switches/1");
        verify(switchClient).encender("http://switches-api:8090/switches/1");
        verify(switchClient, never()).apagar(anyString());
    }

    @Test
    void turnOffSwitchIfAboveExpectedTemp() throws Exception {
        controller.iniciar();
        controller.controlarTemp(22.0, 20.0, "http://switches-api:8090/switches/1");
        verify(switchClient).apagar("http://switches-api:8090/switches/1");
        verify(switchClient, never()).encender(anyString());
    }

    @Test
    void switchUntouchedIfTempEqualsExpected() throws Exception {
        controller.iniciar();
        controller.controlarTemp(20.0, 20.0, "http://switches-api:8090/switches/1");
        verifyNoInteractions(switchClient);
    }

    @Test
    void switchUntouchedIfControllerStopped() throws Exception {
        controller.controlarTemp(18.0, 20.0, "http://switches-api:8090/switches/1");
        verifyNoInteractions(switchClient);
    }

    @Test
    void startControllerTemp() {
        controller.iniciar();
        assertTrue(controller.isActivo());
    }

    @Test
    void stopControllerTemp() {
        controller.iniciar();
        controller.detener();
        assertFalse(controller.isActivo());
    }

    @Test
    void controlarHabShouldTurnSwitchOnIfTempBelowExpected() throws Exception {
        controller.iniciar();
        RoomInfo room = new RoomInfo("1", "Living", 20.0, "termo-1", "http://switches-api:8090/switches/1");

        when(roomProvider.obtenerPorTermostato("termo-1")).thenReturn(Optional.of(room));

        controller.controlarHab(18.0, "termo-1");
        verify(roomProvider).obtenerPorTermostato("termo-1");
        verify(switchClient).encender("http://switches-api:8090/switches/1");
        verify(switchClient, never()).apagar(anyString());
    }

    @Test
    void nSpreadExceptionIfSwitchFails() throws Exception {
        controller.iniciar();

        doThrow(new RuntimeException("Error de conexión")).when(switchClient).encender("http://switches-api:8090/switches/1");
        assertDoesNotThrow(() -> controller.controlarTemp(18.0, 20.0, "http://switches-api:8090/switches/1"));
        verify(switchClient).encender("http://switches-api:8090/switches/1");
    }
}
