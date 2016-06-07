package com.dlmu.bat.client;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public interface TraceStatus {

    /**
     * get current span
     * @return Span
     */
    Span getCurrentSpan();

    /**
     * set current span
     * @param span Span
     */
    void setCurrentSpan(Span span);

    /**
     * remove current span
     */
    void remove();
}
