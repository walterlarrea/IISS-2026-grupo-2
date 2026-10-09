package org.iiss.grupo_2.sub.dto.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class CustomSerializer {
    public static class ThreeDecimalSerializer extends JsonSerializer<Double> {
        @Override
        public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                String formattedValue = "%.3f".formatted(value);
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
