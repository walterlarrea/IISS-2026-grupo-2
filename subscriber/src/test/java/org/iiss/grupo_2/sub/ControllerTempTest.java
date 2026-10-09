package org.iiss.grupo_2.sub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.iiss.grupo_2.sub.dto.Room;
import org.iiss.grupo_2.sub.client.RoomClient;
import org.iiss.grupo_2.sub.client.SwitchClient;
import org.iiss.grupo_2.sub.service.ControllerTempAuto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ControllerTempTest {
    private SwitchClient switchClient;
    private RoomClient roomClient;
    private ControllerTempAuto controller;

    @BeforeEach
    void setUp() {
        switchClient = mock(SwitchClient.class);
        roomClient = mock(RoomClient.class);
        controller = new ControllerTempAuto(switchClient, roomClient);
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
        Room room = mock(Room.class);

        when(room.getTemperaturaEsperada()).thenReturn(20.0);
        when(room.getUriSwitch()).thenReturn("http://switches-api:8090/switches/1");
        when(roomClient.obtenerHabPorTermo("termo-1")).thenReturn(room);

        controller.controlarHab(18.0, "termo-1");
        verify(roomClient).obtenerHabPorTermo("termo-1");
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
