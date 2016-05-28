package com.dlmu.bat.client.proxy;

import com.dlmu.bat.client.BatClient;
import com.dlmu.bat.client.BatClientGetter;
import com.dlmu.bat.client.TraceScope;
import com.dlmu.bat.common.Constants;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public class ProxyUtils {

    /**
     * Returns an object that will trace all calls to itself.
     */
    @SuppressWarnings("unchecked")
    public static <T, V> T createProxy(final T instance) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object obj, Method method, Object[] args)
                    throws Throwable {
                BatClient batClient = BatClientGetter.getClient();
                TraceScope traceScope = batClient.newScope(method.getName());
                try {
                    return method.invoke(instance, args);
                } catch (Throwable e) {
                    traceScope.addKVAnnotation(Constants.EXCEPTION_KEY, "type:" + e.getClass() + ",message:" + e.getMessage());
                    traceScope.addKVAnnotation(Constants.TRACE_STATUS, Constants.TRACE_STATUS_ERROR);
                    throw e;
                } finally {
                    traceScope.close();
                }
            }
        };
        return (T) Proxy.newProxyInstance(instance.getClass().getClassLoader(),
                instance.getClass().getInterfaces(), handler);
    }

}
