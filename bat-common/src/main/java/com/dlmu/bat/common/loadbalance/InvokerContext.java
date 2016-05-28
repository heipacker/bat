package com.dlmu.bat.common.loadbalance;

/**
 * 此次会话（交互）的上下文，一致性Hash需要此标识。
 *
 * @author heipacker
 */
public interface InvokerContext {

    String id();
}
