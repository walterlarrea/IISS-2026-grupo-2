package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.modelos.input.DataSitio;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class EstadoSitioService {
    //conserva el estado actual en memoria, actualiza las temperaturas y registra los estados de los switches
    private DataSitio dataSitio;

    //inicializa o reemplaza el estado del sitio, ejecutar: estadoSitioService.cargarEstado(dataSitioInicial);
    public synchronized void cargarEstado(DataSitio nuevoEstado) {
        if (nuevoEstado == null || nuevoEstado.sitio() == null || nuevoEstado.habitaciones() == null) {
            throw new IllegalArgumentException("El estado del sitio no puede ser nulo");
        }
        this.dataSitio = new DataSitio(nuevoEstado.sitio(), List.copyOf(nuevoEstado.habitaciones()));
    }

    private void validarEstadoInicializado() {
        if (dataSitio == null) {
            throw new IllegalStateException("El estado del sitio todavía no fue inicializado");
        }
    }

    private void reemplazarHabitaciones(List<Habitacion> habitaciones) {
        dataSitio = new DataSitio(dataSitio.sitio(), List.copyOf(habitaciones));
    }

    //devuelve el estado actual del sitio
    public synchronized DataSitio obtenerEstado() {
        if (dataSitio == null) {
            throw new IllegalStateException("El estado del sitio todavía no fue inicializado");
        }
        return dataSitio;
    }

    //actualiza la temperatura recibida de un termostato
    public synchronized DataSitio actualizarTemperatura(String idTermostato, double temperatura) {
        validarEstadoInicializado();
        List<Habitacion> habitaciones = new ArrayList<>(dataSitio.habitaciones());
        boolean encontrada = false;

        if (idTermostato == null || idTermostato.isBlank() || !Double.isFinite(temperatura)) {
            throw new IllegalArgumentException("El termostato y la temperatura deben ser válidos");
        }

        for (int i = 0; i < habitaciones.size(); i++) {
            Habitacion h = habitaciones.get(i);
            if (h.idTermostato().equals(idTermostato)) {
                habitaciones.set(i, new Habitacion(
                        h.id(),
                        h.nombre(),
                        h.temperaturaEsperada(),
                        h.potenciaKW(),
                        h.idTermostato(),
                        h.topicTermostato(),
                        h.idSwitch(),
                        h.urlSwitch(),
                        temperatura,
                        h.estadoActualSwitch()
                ));
                encontrada = true;
                break;
            }
        }

        if (!encontrada) {
            throw new IllegalArgumentException("No existe una habitación asociada al termostato " + idTermostato);
        }
        reemplazarHabitaciones(habitaciones);
        return dataSitio;
    }

    //actualiza el estado conocido de un switch
    public synchronized DataSitio actualizarEstadoSwitch(String idSwitch, EstadoSwitch nuevoEstado) {
        validarEstadoInicializado();
        List<Habitacion> habitaciones = new ArrayList<>(dataSitio.habitaciones());
        boolean encontrado = false;

        if (idSwitch == null || idSwitch.isBlank() || nuevoEstado == null) {
            throw new IllegalArgumentException("El switch y su nuevo estado deben ser válidos");
        }

        for (int i = 0; i < habitaciones.size(); i++) {
            Habitacion h = habitaciones.get(i);
            if (h.idSwitch().equals(idSwitch)) {
                habitaciones.set(i, new Habitacion(
                        h.id(),
                        h.nombre(),
                        h.temperaturaEsperada(),
                        h.potenciaKW(),
                        h.idTermostato(),
                        h.topicTermostato(),
                        h.idSwitch(),
                        h.urlSwitch(),
                        h.temperaturaActual(),
                        nuevoEstado
                ));
                encontrado = true;
                break;
            }
        }

        if (!encontrado) {
            throw new IllegalArgumentException("No existe una habitación asociada al switch " + idSwitch);
        }
        reemplazarHabitaciones(habitaciones);
        return dataSitio;
    }
}


