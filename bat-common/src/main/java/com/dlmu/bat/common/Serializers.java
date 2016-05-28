package com.dlmu.bat.common;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 序列化工具类。
 *
 * @author heipacker
 */
public class Serializers {

    public static final int INT_BYTE_SIZE = 4;
    public static final byte OTHER_TYPE = 0;
    private static final ObjectMapper objectMapper;

    static {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(BaseSpan.class, new SpanJsonSerializer());
        objectMapper = new ObjectMapper().registerModule(simpleModule);
    }

    private static final byte PING_TYPE = 1;
    public static final ByteBuf PING = Unpooled.unreleasableBuffer(Unpooled.directBuffer(1).writeByte(PING_TYPE));
    private static final byte OK_TYPE = 2;
    public static final ByteBuf OK = Unpooled.unreleasableBuffer(Unpooled.directBuffer(1).writeByte(OK_TYPE));

    public static boolean isPing(byte type) {
        return PING_TYPE == type;
    }

    public static boolean isOK(byte type) {
        return OK_TYPE == type;
    }

    public static boolean isOther(byte type) {
        return OTHER_TYPE == type;
    }

    public static String byte2utf8(byte[] in) {
        return new String(in, Charsets.UTF_8);
    }

    public static byte[] utf82byte(String value) {
        return value.getBytes(Charsets.UTF_8);
    }

    public static void writeUTF8String(String value, DataOutputStream output) throws IOException {
        if (value == null) {
            output.writeBoolean(false);
        } else {
            output.writeBoolean(true);
            byte[] result = utf82byte(value);
            output.writeInt(result.length);
            output.write(result);
        }
    }

    public static String readNonASCIIString(DataInputStream input) throws IOException {
        int length = input.readInt();
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append((char) input.read());
        }
        return result.toString();
    }

    public static void writeNonASCIIString(String value, DataOutputStream output) throws IOException {
        int length = value.length();
        output.writeInt(length);
        for (int i = 0; i < length; i++) {
            output.write((byte) value.charAt(i));
        }
    }

    public static void writeBytes(byte[] value, DataOutputStream output) throws IOException {
        output.writeInt(value.length);
        output.write(value);
    }

    public static byte[] readBytes(DataInputStream input) throws IOException {
        int length = input.readInt();
        byte[] value = new byte[length];
        input.read(value);
        return value;
    }

    public static String readUTF8String(DataInputStream input) throws IOException {
        boolean isNotNull = input.readBoolean();
        if (isNotNull) {
            byte[] values = new byte[input.readInt()];
            input.read(values);
            return Serializers.byte2utf8(values);
        } else {
            return null;
        }
    }

    public static byte[] toJsonByteArray(final BaseSpan span) {
        final JsonFactory factory = objectMapper.getFactory();
        final ByteArrayBuilder bb = new ByteArrayBuilder(factory._getBufferRecycler());
        try {
            objectMapper.writeValue(bb, span);
            bb.write('\n');
            return bb.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            bb.release();
        }
    }

    public static class SpanJsonSerializer extends JsonSerializer<BaseSpan> {

        @Override
        public void serialize(BaseSpan value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("traceId", value.traceId);
            jgen.writeStringField("spanId", value.spanId);
            jgen.writeStringField("description", value.description);
            jgen.writeNumberField("start", value.start);
            jgen.writeNumberField("stop", value.stop);
            if (value.traceInfo != null) {
                jgen.writeObjectField("traceInfo", value.traceInfo);
            }
            if (value.timeline != null) {
                jgen.writeObjectField("timeline", value.timeline);
            }
            jgen.writeEndObject();
        }
    }
}
