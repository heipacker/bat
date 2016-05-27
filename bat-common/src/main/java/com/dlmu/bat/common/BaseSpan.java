package com.dlmu.bat.common;

import com.dlmu.bat.common.loadbalance.InvokerContext;
import org.slf4j.MDC;

import java.io.OutputStream;
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

    @Override
    public String id() {
        return traceId;
    }

    public void write(OutputStream os) {
        
    }
}
