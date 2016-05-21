package com.dlmu.bat.common.loadbalance.impl;

import com.dlmu.bat.common.loadbalance.AbstractLoadBalance;
import com.dlmu.bat.common.loadbalance.InvokerContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询实现。
 *
 * @author heipacker
 */
public class RoundRobinLoadBalance<T> extends AbstractLoadBalance<T> {

    private NaturalNumberAtomicCounter sequence = new NaturalNumberAtomicCounter();

    @Override
    protected T doSelect(List<?> sources, InvokerContext context) {
        int index = sequence.getAndIncrement() % sources.size();
        return (T) sources.get(index);
    }
}

/**
 * 自然数(0,1,2...)原子计数器。
 *
 * @author heipacker
 */
class NaturalNumberAtomicCounter {

    private final AtomicInteger atom;
    private static final int mask = 0x7FFFFFFF;


    public NaturalNumberAtomicCounter() {
        atom = new AtomicInteger(0);
    }


    public final int getAndIncrement() {
        final int rt = atom.getAndIncrement();
        return rt & mask;
    }

    public int intValue() {
        return atom.intValue();
    }

}
