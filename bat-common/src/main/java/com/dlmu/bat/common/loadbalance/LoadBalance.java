package com.dlmu.bat.common.loadbalance;

import java.util.List;

/**
 * 负载均衡。
 *
 * @param <T> 元素类型。
 * @author heipacker
 */
public interface LoadBalance<T> {

    T select(List<?> sources, InvokerContext context);
}
