package util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SafeObjectMapper extends ObjectMapper {

    public String writeValueAsString(Object object){
        String result;
        try {
            result = super.writeValueAsString(object);
        }catch (JsonProcessingException e){
            throw new RuntimeException(e);
        }
        return result;
    }
}
