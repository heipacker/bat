package com.dlmu.bat.common;

import com.dlmu.bat.common.loadbalance.InvokerContext;
import com.google.common.base.Charsets;
import org.slf4j.MDC;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public class BaseSpan implements InvokerContext {

    protected String traceId;
    protected String spanId;
    protected String description;
    protected long start;
    protected long stop;
    protected Map<String, String> traceInfo = null;
    protected List<TimelineAnnotation> timeline = null;

    public BaseSpan(String description, String traceId, String spanId) {
        this.description = description;
        this.traceId = traceId;
        this.spanId = spanId;
        //将traceId和spanId都输出到日志
        MDC.put(Constants.MDC_KEY, Constants.TRACE_ID_IN_LOG + "[" + traceId + "]-" + Constants.SPAN_ID_IN_LOG + "[" + spanId + "]");
    }

    public BaseSpan(byte[] sources) throws IOException {
        read(sources);
    }

    public void read(byte[] sources) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(sources));
        byte[] traceIdBytes = new byte[dataInputStream.readInt()];
        dataInputStream.read(traceIdBytes);
        traceId = new String(traceIdBytes);

        byte[] spanIdBytes = new byte[dataInputStream.readInt()];
        dataInputStream.read(spanIdBytes);
        spanId = new String(spanIdBytes);

        start = dataInputStream.readLong();
        stop = dataInputStream.readLong();

        description = readUTF8String(dataInputStream);

        traceInfo = readTraceInfo(dataInputStream);
        timeline = readTimeline(dataInputStream);
    }

    private List<TimelineAnnotation> readTimeline(DataInputStream dataInputStream) throws IOException {
        boolean nullFlag = dataInputStream.readBoolean();
        if (nullFlag) {
            int size = dataInputStream.readInt();
            List<TimelineAnnotation> result = new ArrayList<TimelineAnnotation>();
            for (int i = 0; i < size; ++i) {
                long time = dataInputStream.readLong();
                String message = readUTF8String(dataInputStream);
                result.add(new TimelineAnnotation(time, message));
            }
            return result;
        }
        return null;
    }

    private Map<String, String> readTraceInfo(DataInputStream dataInputStream) throws IOException {
        boolean nullFlag = dataInputStream.readBoolean();
        if (nullFlag) {
            int size = dataInputStream.readInt();
            Map<String, String> traceInfo = new HashMap<String, String>();
            for (int i = 0; i < size; ++i) {
                String key = readUTF8String(dataInputStream);
                String value = readUTF8String(dataInputStream);
                traceInfo.put(key, value);
            }
            return traceInfo;
        }
        return null;
    }

    private String readUTF8String(DataInputStream dataInputStream) throws IOException {
        boolean nullFlag = dataInputStream.readBoolean();
        if (nullFlag) {
            byte[] outputBytes = new byte[dataInputStream.readInt()];
            dataInputStream.read(outputBytes);
            return new String(outputBytes);
        }
        return null;
    }

    public void write(OutputStream os) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(os);
        byte[] traceIdBytes = traceId.getBytes(Charsets.UTF_8);
        dataOutputStream.writeInt(traceIdBytes.length);
        dataOutputStream.write(traceIdBytes);

        byte[] spanIdBytes = spanId.getBytes(Charsets.UTF_8);
        dataOutputStream.writeInt(spanIdBytes.length);
        dataOutputStream.write(spanIdBytes);

        dataOutputStream.writeLong(start);
        dataOutputStream.writeLong(stop);

        writeUTF8String(dataOutputStream, description);

        if (traceInfo == null) {
            dataOutputStream.writeBoolean(false);
        } else {
            dataOutputStream.writeBoolean(true);
            dataOutputStream.writeInt(traceInfo.size());
            for (Map.Entry<String, String> entry : traceInfo.entrySet()) {
                writeUTF8String(dataOutputStream, entry.getKey());
                writeUTF8String(dataOutputStream, entry.getValue());
            }
        }

        if (timeline == null) {
            dataOutputStream.writeBoolean(false);
        } else {
            dataOutputStream.writeBoolean(true);
            dataOutputStream.writeInt(timeline.size());
            for (TimelineAnnotation timelineAnnotation : timeline) {
                dataOutputStream.writeLong(timelineAnnotation.getTime());
                writeUTF8String(dataOutputStream, timelineAnnotation.getMessage());
            }
        }
    }

    private void writeUTF8String(DataOutputStream dataOutputStream, String output) throws IOException {
        if (output == null) {
            dataOutputStream.writeBoolean(false);
        } else {
            dataOutputStream.writeBoolean(true);
            byte[] outputBytes = output.getBytes(Charsets.UTF_8);
            dataOutputStream.writeInt(outputBytes.length);
            dataOutputStream.write(outputBytes);
        }
    }

    @Override
    public String id() {
        return traceId;
    }

}
