package com.iiss.sensores.grupo_2.controlador.api.config;

import com.iiss.sensores.grupo_2.controlador.api.model.ErrorApi;
import com.iiss.sensores.grupo_2.controlador.api.service.InvalidInventoryException;
import com.iiss.sensores.grupo_2.controlador.api.service.SiteNotConfigException;
import com.iiss.sensores.grupo_2.controlador.api.service.InvalidRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorApi> handleBadRequest(Exception exception) {
        List<String> errores = exception instanceof MethodArgumentNotValidException validationException
                ? validationException.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .toList()
                : List.of("El cuerpo de la petición no tiene un formato válido");
        return error(HttpStatus.BAD_REQUEST, "solicitud inválida", errores);
    }

    @ExceptionHandler(InvalidInventoryException.class)
    public ResponseEntity<ErrorApi> handleInventarioInvalido(InvalidInventoryException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), exception.getErrores());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorApi> handleSolicitudInvalida(InvalidRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), exception.getErrores());
    }

    @ExceptionHandler(SiteNotConfigException.class)
    public ResponseEntity<ErrorApi> handleSitioNoConfigurado(SiteNotConfigException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    private ResponseEntity<ErrorApi> error(HttpStatus status, String mensaje, List<String> errores) {
        return ResponseEntity.status(status).body(new ErrorApi(
                Instant.now(),
                status.value(),
                mensaje,
                errores
        ));
    }
}
