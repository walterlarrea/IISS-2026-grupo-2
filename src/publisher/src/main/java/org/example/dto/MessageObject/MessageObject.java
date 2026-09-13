package org.example.dto.MessageObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.example.dto.serializer.CustomSerializer.TwoDecimalSerializer;
import org.example.dto.serializer.CustomSerializer.ThreeDecimalSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageObject {
    private static final Logger logger = LoggerFactory.getLogger(MessageObject.class);

    public record Temperatura(
            int id,

            @JsonSerialize(using = TwoDecimalSerializer.class)
            float tC,

            @JsonSerialize(using = TwoDecimalSerializer.class)
            float tF,

            @JsonSerialize(using = ThreeDecimalSerializer.class)
            float ts
    ){}

    public static <Type> String validateJson(Type object) throws Exception {
        try{

            ObjectMapper mapper = new ObjectMapper();

            return mapper.writeValueAsString(object);
        }catch(JsonProcessingException e){
            logger.error("Failed to validate JSON object of type {}", object.getClass().getName());
            throw new Exception("Failed to validate JSON object");
        }
    }
}
