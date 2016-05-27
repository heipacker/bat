package com.dlmu.bat.common;

/**
 * Created by heipacker on 16-5-18.
 */
public class Constants {

    /**
     * 存放文件的目录
     */
    public static final String DTRACER_CONFIG_FILE = "META-INF/dtracer-annotation";

    public static final String SERVER_ROOT = "/dtrace/server/root";

    //日志文件里显示的信息
    public static final String MDC_KEY = "DTRACER";
    public static final String TRACE_ID_IN_LOG = "DTraceId";
    public static final String SPAN_ID_IN_LOG = "QSpanId";

    public static final String ROOT_SPANID = "1";


    public static final String TRACE_WATCHID = "watchid";

    public static final String TRACE_TYPE = "TRACE_TYPE";

    public static final String EXCEPTION_KEY = "exception";

    public static final String TRACE_STATUS = "TRACE_STATUS";
    public static final String TRACE_STATUS_OK = "OK";
    public static final String TRACE_STATUS_ERROR = "ERROR";

    public static final String NO_NEW_TRACEID = "NO_NEW_TRACEID";

}
