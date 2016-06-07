package com.dlmu.bat.client.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class TraceExecutorService implements ExecutorService {

    private final String description;
    private final ExecutorService impl;

    public TraceExecutorService(ExecutorService impl) {
        this(null, impl);
    }

    public TraceExecutorService(String description, ExecutorService impl) {
        this.description = description;
        this.impl = impl;
    }

    @Override
    public void execute(Runnable command) {
        impl.execute(ConcurrentUtils.wrap(command, description));
    }

    @Override
    public void shutdown() {
        impl.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return impl.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return impl.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return impl.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return impl.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return impl.submit(ConcurrentUtils.wrap(task, description));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return impl.submit(ConcurrentUtils.wrap(task, description), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return impl.submit(ConcurrentUtils.wrap(task, description));
    }

    private <T> Collection<? extends Callable<T>> wrapCollection(Collection<? extends Callable<T>> tasks) {
        List<Callable<T>> result = new ArrayList<Callable<T>>();
        for (Callable<T> task : tasks) {
            result.add(ConcurrentUtils.wrap(task, description));
        }
        return result;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return impl.invokeAll(wrapCollection(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return impl.invokeAll(wrapCollection(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return impl.invokeAny(wrapCollection(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return impl.invokeAny(wrapCollection(tasks), timeout, unit);
    }

}
