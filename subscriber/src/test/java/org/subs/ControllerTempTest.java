package org.subs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.subs.classes.Room;
import org.subs.client.RoomClient;
import org.subs.client.SwitchClient;
import org.subs.controllertemp.ControllerTempAuto;
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
    void turnOnSwitchIfBelowExpectedTemp() throws Exception {//para prender el switch si la temperatura está por debajo de la esperada.
        controller.iniciar();
        controller.controlarTemp(18.0, 20.0, "http://switches-api:8090/switches/1");
        verify(switchClient).encender("http://switches-api:8090/switches/1");
        verify(switchClient, never()).apagar(anyString());
    }

    @Test
    void turnOffSwitchIfAboveExpectedTemp() throws Exception {//para apagar el switch si la temperatura está por arriba de la esperada.
        controller.iniciar();
        controller.controlarTemp(22.0, 20.0, "http://switches-api:8090/switches/1");
        verify(switchClient).apagar("http://switches-api:8090/switches/1");
        verify(switchClient, never()).encender(anyString());
    }

    @Test
    void switchUntouchedIfTempEqualsExpected() throws Exception {//para que el switch no se accione si la temperatura es la esperada.
        controller.iniciar();
        controller.controlarTemp(20.0, 20.0, "http://switches-api:8090/switches/1");
        verifyNoInteractions(switchClient);
    }

    @Test
    void switchUntouchedIfControllerStopped() throws Exception {//para que el switch no se accione si el controlador está detenido.
        //el controlador comienza detenido
        controller.controlarTemp(18.0, 20.0, "http://switches-api:8090/switches/1");
        verifyNoInteractions(switchClient);
    }

    @Test
    void startControllerTemp() {//para comprobar si se inicia el controlador.
        controller.iniciar();
        assertTrue(controller.isActivo());
    }

    @Test
    void stopControllerTemp() {//para comprobar que se detiene el controlador.
        controller.iniciar();
        controller.detener();
        assertFalse(controller.isActivo());
    }

    @Test
    void controlarHabShouldTurnSwitchOnIfTempBelowExpected() throws Exception {//para comprobar si se enciende el switch correcto de una habitación asociada si la temperatura es menor a la esperada.
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
    void nSpreadExceptionIfSwitchFails() throws Exception {//para comprobar el manejo de errores.
        controller.iniciar();

        doThrow(new RuntimeException("Error de conexión")).when(switchClient).encender("http://switches-api:8090/switches/1");
        assertDoesNotThrow(() -> controller.controlarTemp(18.0, 20.0, "http://switches-api:8090/switches/1"));
        verify(switchClient).encender("http://switches-api:8090/switches/1");
    }

}
