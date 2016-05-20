package com.dlmu.bat.common.loadbalance;

import java.util.List;

/**
 * 负载均衡。
 * 
 * @author heipacker
 *
 * @param <T> 元素类型。
 */
public interface LoadBalance<T> {

	T select(List<?> sources, InvokerContext context);
}
