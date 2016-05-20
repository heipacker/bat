package com.dlmu.bat.common.loadbalance;

import java.util.List;

/**
 * 抽象负载均衡。处理空情况。
 * 
 * @author heipacker
 *
 * @param <T> 元素类型。
 */
public abstract class AbstractLoadBalance<T> implements LoadBalance<T> {

	@Override
	public T select(List<?> sources, InvokerContext context) {
		if (sources == null || sources.isEmpty()) {
			return null;
		}
		return doSelect(sources, context);
	}
	
	protected abstract T doSelect(List<?> sources, InvokerContext context);

}
