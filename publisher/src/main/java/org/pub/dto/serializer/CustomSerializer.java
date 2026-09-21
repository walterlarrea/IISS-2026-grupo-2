package org.pub.dto.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class CustomSerializer {
    public static class ThreeDecimalSerializer extends JsonSerializer<Double> {
        @Override
        public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                // Aplica formato "%.3f" y escribe el resultado como texto
                String formattedValue = "%.3f".formatted(value);
                // Escribe el número ya formateado para conservar la notación decimal
                gen.writeNumber(formattedValue.replace(',', '.'));
            } else {
                gen.writeNull();
            }
        }
    }

    public static class TwoDecimalSerializer extends JsonSerializer<Double> {
        @Override
        public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                String formattedValue = "%.2f".formatted(value);
                gen.writeNumber(formattedValue.replace(',', '.'));
            } else {
                gen.writeNull();
            }
        }
    }
}
