package com.dlmu.bat.client.receiver;

import com.dlmu.bat.client.Span;
import com.dlmu.bat.common.BaseSpan;
import com.dlmu.bat.common.ClassUtils;
import com.dlmu.bat.common.conf.ConfigConstants;
import com.dlmu.bat.common.conf.DTraceConfiguration;
import com.dlmu.bat.common.loadbalance.LoadBalance;
import com.dlmu.bat.common.loadbalance.Node;

import java.io.IOException;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public class RPCSpanReceiver extends SpanReceiver {

    private LoadBalance<Node> loadBalance;

    public RPCSpanReceiver(DTraceConfiguration configuration){
        initLoadBalance(configuration);
    }

    private void initLoadBalance(DTraceConfiguration configuration) {
        String loadBalanceClass = configuration.get(ConfigConstants.RECEIVER_LOAD_BALANCE_KEY, ConfigConstants.DEFAULT_RECEIVER_LOAD_BALANCE);
        loadBalance = (LoadBalance) ClassUtils.newInstance(loadBalanceClass);
    }

    /**
     * Called when a Span is stopped and can now be stored.
     *
     * @param span The span to store with this SpanReceiver.
     */
    @Override
    public void receiveSpan(BaseSpan span) {
        loadBalance.select(null, span);
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     * <p>
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {

    }
}
