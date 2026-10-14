package com.iiss.sensores.grupo_2.controlador.engine.service;

import com.iiss.sensores.grupo_2.interfaces.ICore;
import com.iiss.sensores.grupo_2.enums.EstadoSwitch;
import com.iiss.sensores.grupo_2.modelos.input.DataSitio;
import com.iiss.sensores.grupo_2.modelos.input.Habitacion;
import com.iiss.sensores.grupo_2.modelos.output.Comando;
import com.iiss.sensores.grupo_2.modelos.output.ComandoSwitch;
import com.iiss.sensores.grupo_2.modelos.output.RespuestaCore;
import com.iiss.sensores.grupo_2.controlador.engine.client.SwitchClient;
import com.iiss.sensores.grupo_2.controlador.api.model.EstadoControl;
import com.iiss.sensores.grupo_2.controlador.shared.TemperatureEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class ControllerTempAuto implements TemperatureEngineService {
    private static final Logger logger = LoggerFactory.getLogger(ControllerTempAuto.class);
    private volatile boolean activo = false;
    private Instant fechaHoraBase;
    private Instant iniciadoEn;
    private double factorTiempo;
    private final SwitchClient switchClient;
    private final ICore core;
    private final EstadoSitioService estadoSitioService; //mantiene los datos actuales del sitio

    public ControllerTempAuto(SwitchClient switchClient, ICore core, EstadoSitioService estadoSitioService) {
        //conecta el estado con el core, ejecuta los comandos calculados y controla el ciclo de ejecución.
        this.switchClient = switchClient;
        this.core = core;
        this.estadoSitioService = estadoSitioService;
    }

    @Override
    public synchronized void iniciar() {
        iniciar(Instant.now(), 1.0);
    }

    @Override
    public synchronized void iniciar(Instant fechaHora, double factorTiempo) {
        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha y hora de inicio es obligatoria");
        }

        if (!Double.isFinite(factorTiempo) || factorTiempo <= 0) {
            throw new IllegalArgumentException("El factor de tiempo debe ser positivo y finito");
        }

        this.fechaHoraBase = fechaHora;
        this.iniciadoEn = Instant.now();
        this.factorTiempo = factorTiempo;
        this.activo = true;

        logger.info("Controlador iniciado. Fecha simulada: {}, factor: {}", fechaHora, factorTiempo);
    }

    @Override
    public synchronized void detener() {
        activo = false;
        fechaHoraBase = null;
        iniciadoEn = null;
        factorTiempo = 0.0;

        logger.info("Controlador detenido");
    }

    @Override
    public boolean isActivo() {
        return activo;
    }

    @Override
    public synchronized EstadoControl obtenerEstado() {
        if (!activo) {
            return new EstadoControl(EstadoControl.Estado.DETENIDO, null, null, null);
        }
        long tiempoRealNanos = Duration.between(iniciadoEn, Instant.now()).toNanos();
        Instant fechaHoraActual = fechaHoraBase.plusNanos((long) (tiempoRealNanos * factorTiempo));
        return new EstadoControl(EstadoControl.Estado.EN_EJECUCION, fechaHoraActual, factorTiempo, iniciadoEn);
    }

    //solicita las decisiones al core y ejecuta sus comandos, todo el ciclo se serializa para evitar decisiones simultáneas.
    public synchronized void controlar() {
        if (!activo) {
            logger.debug("Controlador detenido");
            return;
        }

        try {
            DataSitio dataSitio = estadoSitioService.obtenerEstado();
            EstadoControl estado = obtenerEstado();
            ZonedDateTime fechaSimulada = estado.fechaHora().atZone(ZoneId.systemDefault());
            RespuestaCore respuesta = core.calcularComandos(fechaSimulada, dataSitio);

            if (respuesta == null || respuesta.comandos() == null) {
                logger.warn("El core devolvió una respuesta nula");
                return;
            }

            for (Comando comando : respuesta.comandos()) {
                if (!(comando instanceof ComandoSwitch comandoSwitch)) {
                    logger.debug("Se omite un comando que no es de tipo switch");
                    continue;
                }

                Habitacion habitacion = dataSitio.habitaciones().stream()
                        .filter(h -> h.idSwitch().equals(comandoSwitch.idSwitch()))
                        .findFirst()
                        .orElse(null);

                if (habitacion == null) {
                    logger.warn("No se encontró habitación para el switch {}", comandoSwitch.idSwitch());
                    continue;
                }

                try {
                    if (comandoSwitch.estado() == EstadoSwitch.ENCENDIDO) {
                        switchClient.encender(habitacion);
                        logger.info("Estado: encendido");
                    } else if (comandoSwitch.estado() == EstadoSwitch.APAGADO) {
                        switchClient.apagar(habitacion);
                        logger.info("Estado: apagado");
                    } else {
                        logger.warn("Estado de switch no reconocido: {}", comandoSwitch.estado());
                        continue;
                    }

                    //solo se actualiza el estado local si la operación remota terminó sin lanzar una excepción.
                    estadoSitioService.actualizarEstadoSwitch(comandoSwitch.idSwitch(), comandoSwitch.estado());
                    logger.info("Switch {} actualizado a {}", comandoSwitch.idSwitch(), comandoSwitch.estado());

                } catch (Exception e) {
                    logger.error("Error al ejecutar el comando del switch {} con url {}", comandoSwitch.idSwitch(), habitacion.urlSwitch(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error durante el ciclo de control", e);
        }
    }

    //actualiza una temperatura y ejecuta un nuevo ciclo de control.
    public synchronized void procesarTemperatura(String topic, double temperatura) {
        if (!activo) {
            logger.debug("Controlador detenido, ignora la temperatura del topic {}", topic);
            return;
        }

        if (topic == null || topic.isBlank() || !Double.isFinite(temperatura)) {
            logger.warn("Medición inválida. Topic: {}, temperatura: {}", topic, temperatura);
            return;
        }

        try {
            estadoSitioService.actualizarTemperatura(topic, temperatura);
            controlar();

        } catch (Exception e) {
            logger.error("Error al procesar la temperatura del topic {}", topic, e);
        }
    }

}
