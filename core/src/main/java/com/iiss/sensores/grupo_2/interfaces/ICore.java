package com.iiss.sensores.grupo_2.interfaces;


import com.iiss.sensores.grupo_2.modelos.output.RespuestaCore;
import com.iiss.sensores.grupo_2.modelos.input.DataSitio;

import java.time.ZonedDateTime;

public interface ICore {
    RespuestaCore calcularComandos(ZonedDateTime fechaHora, DataSitio dataSitio);
}

// (sitio, instant, lectura <- opt)
