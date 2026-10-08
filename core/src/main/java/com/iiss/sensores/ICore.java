package com.iiss.sensores;


import com.iiss.sensores.modelos.RespuestaCore;
import com.iiss.sensores.modelos.input.DataSitio;

import java.time.ZonedDateTime;

public interface ICore {
    RespuestaCore calcularComandos(ZonedDateTime fechaHora, DataSitio dataSitio);
}

// (sitio, instant, lectura <- opt)
