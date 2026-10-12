package com.iiss.sensores.grupo_2.controlador.api.service;

import com.iiss.sensores.grupo_2.controlador.api.model.Inventario;
import com.iiss.sensores.grupo_2.controlador.api.model.InventarioDocumento;
import com.iiss.sensores.grupo_2.controlador.api.repository.InventarioRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class InventarioService {
    private static final String INVENTARIO_ACTIVO_ID = "activo";

    private final InventarioRepository repository;

    public InventarioService(InventarioRepository repository) {
        this.repository = repository;
    }

    public Optional<Inventario> obtenerActivo() {
        return repository.findById(INVENTARIO_ACTIVO_ID).map(InventarioDocumento::inventario);
    }

    @CacheEvict(value = "habitaciones_termostato", allEntries = true)
    public Inventario reemplazar(Inventario inventario) {
        validar(inventario);
        repository.save(new InventarioDocumento(INVENTARIO_ACTIVO_ID, inventario));
        return inventario;
    }

    private void validar(Inventario inventario) {
        List<String> errores = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<String> termostatos = new HashSet<>();
        Set<String> switches = new HashSet<>();

        for (Inventario.Habitacion habitacion : inventario.habitaciones()) {
            if (!ids.add(habitacion.id())) {
                errores.add("id duplicado: '" + habitacion.id() + "'");
            }
            if (!termostatos.add(habitacion.idTermostato())) {
                errores.add("idTermostato duplicado: '" + habitacion.idTermostato() + "'");
            }
            if (!switches.add(habitacion.idSwitch())) {
                errores.add("idSwitch duplicado: '" + habitacion.idSwitch() + "'");
            }
            if (!esUrlValida(habitacion.urlSwitch())) {
                errores.add("habitación '" + habitacion.id() + "': urlSwitch debe ser una URL HTTP o HTTPS válida");
            }
            if (!Double.isFinite(habitacion.temperaturaEsperada())) {
                errores.add("habitación '" + habitacion.id() + "': temperaturaEsperada debe ser finita");
            }
            if (!Double.isFinite(habitacion.potenciaKW()) || habitacion.potenciaKW() <= 0) {
                errores.add("habitación '" + habitacion.id() + "': potenciaKW debe ser > 0");
            }
        }

        if (!Double.isFinite(inventario.sitio().potenciaContratadaKW())
                || inventario.sitio().potenciaContratadaKW() <= 0) {
            errores.add("sitio: potenciaContratadaKW debe ser > 0");
        }
        Inventario.Franja punta = inventario.sitio().tarifa().punta();
        if (!esFranjaValida(punta)) {
            errores.add("tarifa.punta.desde debe ser anterior a tarifa.punta.hasta");
        }

        if (!errores.isEmpty()) {
            throw new InventarioInvalidoException(errores);
        }
    }

    private boolean esFranjaValida(Inventario.Franja franja) {
        try {
            return LocalTime.parse(franja.desde()).isBefore(LocalTime.parse(franja.hasta()));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean esUrlValida(String valor) {
        try {
            URI uri = URI.create(valor);
            return uri.isAbsolute()
                    && uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
