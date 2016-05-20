package com.dlmu.bat.client;

import com.dlmu.bat.common.transformer.Constants;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TraceUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, true);
    }

    public static final TraceInfo NEW_NO_TRACE = new TraceInfo(Constants.NO_NEW_TRACEID, "");

    public static void addKVAnnotation(TraceScope scope, String key, Object value) {
        if (value == null) return;
        String temp = null;
        try {
            temp = objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return;
        }
        if (temp == null) return;
        scope.addKVAnnotation(key, temp);
    }

    public static void addKVAnnotation(TraceScope scope, String key, boolean value) {
        scope.addKVAnnotation(key, String.valueOf(value));
    }

    public static void addKVAnnotation(TraceScope scope, String key, byte value) {
        scope.addKVAnnotation(key, String.valueOf(value));
    }

    public static void addKVAnnotation(TraceScope scope, String key, char value) {
        scope.addKVAnnotation(key, String.valueOf(value));
    }


    public static void addKVAnnotation(TraceScope scope, String key, int value) {
        scope.addKVAnnotation(key, String.valueOf(value));
    }

    public static void addKVAnnotation(TraceScope scope, String key, long value) {
        scope.addKVAnnotation(key, String.valueOf(value));
    }

    public static void addKVAnnotation(TraceScope scope, String key, double value) {
        scope.addKVAnnotation(key, String.valueOf(value));
    }

    public static void addKVAnnotation(TraceScope scope, String key, float value) {
        scope.addKVAnnotation(key, String.valueOf(value));
    }

    public static void addKVAnnotation(TraceScope scope, String key, short value) {
        scope.addKVAnnotation(key, String.valueOf(value));
    }

}
