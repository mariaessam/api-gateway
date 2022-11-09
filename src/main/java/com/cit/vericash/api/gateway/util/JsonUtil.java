package com.cit.vericash.api.gateway.util;

import com.google.gson.*;
import org.apache.commons.lang.StringEscapeUtils;
import java.lang.reflect.Type;
import java.math.BigDecimal;

public class JsonUtil {
    private static Gson gson;

    static{
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(Double.class,  new JsonSerializer<Double>() {
            @Override
            public JsonElement serialize(final Double src, final Type typeOfSrc, final JsonSerializationContext context) {
                BigDecimal value = BigDecimal.valueOf(src);
                try {
                    value = new BigDecimal(value.toBigIntegerExact());
                } catch (ArithmeticException e) {
                }
                return new JsonPrimitive(value);
            }
        });
        gson=gsonBuilder.create();
    }

    public static String getJsonForPrinting(Object object){
        if(object!=null){
            Gson gson = getGson();
            String jsonStr = gson.toJson(object);
            jsonStr = excludeDotZeroForInts(jsonStr);
            return StringEscapeUtils.unescapeJava(jsonStr);
        }
        return null;
    }

    public static String excludeDotZeroForInts(String jsonStr){
        if(jsonStr!=null){
            return jsonStr.replaceAll("(\"\\S*\")(\\s*):(\\s*)([^\"]\\d*)\\.0(?!\\d)(\\n?\\r?)","$1: $4$5");
        }
        return null;
    }

    public static Gson getGson(){
        return gson;
    }
}
