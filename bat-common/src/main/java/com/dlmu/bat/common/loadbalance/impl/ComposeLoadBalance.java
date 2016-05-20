package com.dlmu.bat.common.loadbalance.impl;

import com.dlmu.bat.common.loadbalance.AbstractLoadBalance;
import com.dlmu.bat.common.loadbalance.InvokerContext;
import com.dlmu.bat.common.loadbalance.LoadBalance;

import java.util.List;

/**
 * 不搞单独的策略多路了，通过继承实现多路。
 *
 * @author heipacker
 */
public abstract class ComposeLoadBalance<T> extends AbstractLoadBalance<T> {

    private LoadBalance<T>[] loadBalances;

    public ComposeLoadBalance(LoadBalance<T>... loadBalances) {
        this.loadBalances = loadBalances;
    }

    @Override
    protected T doSelect(List<?> sources, InvokerContext context) {
        for (LoadBalance<T> loadBalance : loadBalances) {
            T target = loadBalance.select(sources, context);
            if (checkPostPartSelect(loadBalance, target)) {
                return target;
            }
        }
        return null;
    }

    protected abstract boolean checkPostPartSelect(LoadBalance<T> loadBalance, T target);
}
