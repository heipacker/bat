package com.dlmu.bat.common;

/**
 * Created by heipacker on 16-5-18.
 */
public class Constants {

    /**
     * 存放文件的目录
     */
    public static final String BAT_TRACER_CONFIG_FILE = "META-INF/bat-tracer-annotation";

    public static final String SERVER_ROOT = "/battrace/server/root";

    //日志文件里显示的信息
    public static final String MDC_KEY = "BATTRACER";
    public static final String TRACE_ID_IN_LOG = "BatTraceId";
    public static final String SPAN_ID_IN_LOG = "BatSpanId";

    public static final String ROOT_SPANID = "1";


    public static final String TRACE_WATCHID = "watchid";

    public static final String TRACE_TYPE = "TRACE_TYPE";

    public static final String EXCEPTION_KEY = "exception";

    public static final String TRACE_STATUS = "TRACE_STATUS";
    public static final String TRACE_STATUS_OK = "OK";
    public static final String TRACE_STATUS_ERROR = "ERROR";

    public static final String NO_NEW_TRACEID = "NO_NEW_TRACEID";


    //traceInfo
    public static final String BAT_TRACE_TNAME = "tname";
    public static final String BAT_TRACE_LOCAL_HOSTADDRESS = "T_LOCAL_HOSTADDRESS";
    public static final String BAT_TRACE_LOCAL_HOSTNAME = "T_LOCAL_HOSTNAME";


}
