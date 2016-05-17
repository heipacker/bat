package com.dlmu.agent.asm;

import com.dlmu.agent.annotation.DP;
import com.dlmu.agent.annotation.DTrace;
import com.dlmu.agent.server.transformer.Constants;
import org.apache.htrace.core.TraceScope;
import org.apache.htrace.core.Tracer;

/**
 * Created by heipacker on 16-5-15.
 */
public class AsmTestClass {

    public Object test() {
        Tracer tracer = new Tracer.Builder().name("test").build();
        TraceScope traceScope = tracer.newScope("test");
        try {
            return test1();
        } catch (RuntimeException e) {
            traceScope.addKVAnnotation(Constants.EXCEPTION_KEY, "type:" + e.getClass() + ",message:" + e.getMessage());
            traceScope.addKVAnnotation(Constants.TRACE_STATUS, Constants.TRACE_STATUS_ERROR);
            throw e;
        } finally {
            traceScope.close();
        }
    }

    public Object test1() {
        System.out.println("test1");
        return "";
    }

    @DTrace
    public Object testQAnnotation(@DP String str1) {
        System.out.println("test1");
        return "";
    }

}
