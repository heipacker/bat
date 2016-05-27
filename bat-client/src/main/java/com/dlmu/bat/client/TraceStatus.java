package com.dlmu.bat.client;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public interface TraceStatus {

    Span getCurrentSpan();

    void setCurrentSpan(Span t);

    void remove();
}
