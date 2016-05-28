package com.dlmu.test.asm;

import com.dlmu.bat.annotation.DP;
import com.dlmu.bat.annotation.DTrace;
import com.dlmu.bat.client.DTraceClient;
import com.dlmu.bat.client.DTraceClientGetter;
import com.dlmu.bat.client.TraceScope;
import com.dlmu.bat.client.TraceUtils;
import com.dlmu.bat.common.Constants;

/**
 * @author heipacker on 16-5-15.
 */
public class AsmTestClass {

    public Object test() {
        DTraceClient traceClient = DTraceClientGetter.getClient();
        TraceScope newScope = traceClient.newScope("test", TraceUtils.NEW_NO_TRACE);
        try {
            return test1();
        } catch (RuntimeException e) {
            newScope.addKVAnnotation(Constants.EXCEPTION_KEY, "type:" + e.getClass() + ",message:" + e.getMessage());
            newScope.addKVAnnotation(Constants.TRACE_STATUS, Constants.TRACE_STATUS_ERROR);
            throw e;
        } finally {
            newScope.close();
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
