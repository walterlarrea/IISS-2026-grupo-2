package com.iiss.sensores.grupo_2.interfaces;

import com.iiss.sensores.grupo_2.modelos.output.CoreAnswer;
import com.iiss.sensores.grupo_2.modelos.input.DataSite;
import java.time.ZonedDateTime;

public interface ICore {
    CoreAnswer calcularComandos(ZonedDateTime fechaHora, DataSite dataSite);
}

