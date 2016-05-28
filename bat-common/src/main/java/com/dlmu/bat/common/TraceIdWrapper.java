package com.dlmu.bat.common;

import org.apache.commons.lang3.StringUtils;

import static com.dlmu.bat.common.Constants.BAT_TRACE_SPLITTER_STR;
import static com.dlmu.bat.common.Constants.BAT_TRACE_UNIQUE_SPLITTER_STR;

/**
 * @author heipacker
 * @date 16-5-28.
 */
public class TraceIdWrapper {

    private String tname;

    private String date;

    private String time;

    private String ip;

    private String pid;

    private String sequence;

    private String suffix;

    /**
     * @param traceId
     * @return
     */
    public static TraceIdWrapper parseTraceId(String traceId) {
        TraceIdWrapper traceIdWrapper = new TraceIdWrapper();
        int index1 = traceId.indexOf(BAT_TRACE_SPLITTER_STR);
        traceIdWrapper.tname = traceId.substring(0, index1);
        String traceIdSubStr = traceId.substring(index1 + 1);
        int index2 = traceIdSubStr.indexOf(BAT_TRACE_SPLITTER_STR);
        traceIdWrapper.suffix = traceIdSubStr.substring(index2 + 1);
        traceIdSubStr = traceIdSubStr.substring(0, index2);

        //parse datetime ip pid sequence
        String[] elements = StringUtils.split(traceIdSubStr, BAT_TRACE_UNIQUE_SPLITTER_STR);
        traceIdWrapper.date = elements[0];
        traceIdWrapper.time = elements[1];
        traceIdWrapper.ip = elements[2] + BAT_TRACE_UNIQUE_SPLITTER_STR +
                elements[3] + BAT_TRACE_UNIQUE_SPLITTER_STR +
                elements[4] + BAT_TRACE_UNIQUE_SPLITTER_STR +
                elements[5];
        traceIdWrapper.pid = elements[6];
        traceIdWrapper.sequence = elements[7];
        return traceIdWrapper;
    }

    public String rowKey() {
        return StringUtils.reverse(sequence) + "." + time + "." + date + "." + pid + "." + ip + "_" + tname + suffix;
    }

    public String getTname() {
        return tname;
    }

    public void setTname(String tname) {
        this.tname = tname;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TraceIdWrapper that = (TraceIdWrapper) o;

        if (tname != null ? !tname.equals(that.tname) : that.tname != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (time != null ? !time.equals(that.time) : that.time != null) return false;
        if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
        if (pid != null ? !pid.equals(that.pid) : that.pid != null) return false;
        if (sequence != null ? !sequence.equals(that.sequence) : that.sequence != null) return false;
        return suffix != null ? suffix.equals(that.suffix) : that.suffix == null;

    }

    @Override
    public int hashCode() {
        int result = tname != null ? tname.hashCode() : 0;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (ip != null ? ip.hashCode() : 0);
        result = 31 * result + (pid != null ? pid.hashCode() : 0);
        result = 31 * result + (sequence != null ? sequence.hashCode() : 0);
        result = 31 * result + (suffix != null ? suffix.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TraceIdWrapper{" +
                "tname='" + tname + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", ip='" + ip + '\'' +
                ", pid='" + pid + '\'' +
                ", sequence='" + sequence + '\'' +
                ", suffix='" + suffix + '\'' +
                '}';
    }
}
