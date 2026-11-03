package com.iiss.sensores.grupo_2.interfaces;

import com.iiss.sensores.grupo_2.modelos.output.CoreAnswer;
import com.iiss.sensores.grupo_2.modelos.input.DataSite;
import java.time.ZonedDateTime;

/** Define el contrato para evaluar el sitio y producir comandos de control. */
public interface ICore {
  /** Calcula los comandos necesarios según la hora y el estado del sitio. */
  CoreAnswer calcularComandos(ZonedDateTime dateTime, DataSite siteData);
}
