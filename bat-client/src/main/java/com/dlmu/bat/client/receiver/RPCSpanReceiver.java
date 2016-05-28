package com.dlmu.bat.client.receiver;

import com.dlmu.bat.common.BaseSpan;
import com.dlmu.bat.common.ClassUtils;
import com.dlmu.bat.common.Constants;
import com.dlmu.bat.common.conf.ConfigConstants;
import com.dlmu.bat.common.loadbalance.LoadBalance;
import com.dlmu.bat.common.loadbalance.Node;
import com.dlmu.bat.common.netty.NettyClient;
import com.dlmu.bat.plugin.conf.Configuration;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections.CollectionUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author heipacker
 * @date 16-5-27.
 */
public class RPCSpanReceiver extends SpanReceiver {

    public static final int DEFAULT_BATCH_SIZE = 100;

    public static final int DEFAULT_QUEUE_SIZE = 1000;
    public static final Function<BaseSpanNettyClient<BaseSpan>, String> SPAN_NETTY_CLIENT_ID_FUNCTION = new Function<BaseSpanNettyClient<BaseSpan>, String>() {
        @Override
        public String apply(BaseSpanNettyClient<BaseSpan> input) {
            return input.id();
        }
    };

    private Configuration configuration;

    private LoadBalance<BaseSpanNettyClient> loadBalance;

    private ZkClient zkClient;

    private volatile int arrayBlockingQueueSize = DEFAULT_QUEUE_SIZE;

    private volatile int batchSize = DEFAULT_BATCH_SIZE;

    private List<BaseSpanNettyClient<BaseSpan>> nettyClientList = Lists.newArrayList();

    public RPCSpanReceiver(Configuration configuration) {
        this.configuration = configuration;
        initLoadBalance(configuration);
        this.zkClient = new ZkClient(configuration.get(ConfigConstants.ZK_ADDRESS_KEY, ConfigConstants.DEFAULT_ZK_ADDRESS));
        List<String> children = zkClient.getChildren(Constants.SERVER_ROOT);
        if (CollectionUtils.isNotEmpty(children)) {
            initRpcClient(children);
        }
        configuration.addListener(new Configuration.ConfigurationListener() {
            @Override
            public void call(Configuration configuration) {
                arrayBlockingQueueSize = configuration.getInt("bat.client.base.span.queue.size", DEFAULT_QUEUE_SIZE);
                batchSize = configuration.getInt("bat.client.base.span.batch.size", DEFAULT_BATCH_SIZE);

            }
        }, true);
        this.zkClient.subscribeChildChanges(Constants.SERVER_ROOT, new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                initRpcClient(currentChilds);
            }
        });
    }

    private void initRpcClient(List<String> children) {
        Set<String> currentServerSet = FluentIterable.from(nettyClientList).transform(SPAN_NETTY_CLIENT_ID_FUNCTION).toSet();
        Set<String> changedServerSet = Sets.newHashSet(children);
        //已经不在children里的server
        for (String server : Sets.difference(currentServerSet, changedServerSet)) {
            Iterator<BaseSpanNettyClient<BaseSpan>> iterator = nettyClientList.iterator();
            while (iterator.hasNext()) {
                BaseSpanNettyClient<BaseSpan> nettyClient = iterator.next();
                if (Objects.equal(nettyClient.id(), server)) {
                    nettyClient.close();
                    iterator.remove();
                }
            }
        }
        //多出来的
        for (String server : Sets.difference(changedServerSet, currentServerSet)) {
            NettyClient.Config config = new NettyClient.ConfigBuilder().setServer(server).build();
            ArrayBlockingQueue<BaseSpan> baseSpanArrayBlockingQueue = new ArrayBlockingQueue<>(arrayBlockingQueueSize);
            BaseSpanNettyClient<BaseSpan> nettyClient = new BaseSpanNettyClient<BaseSpan>(baseSpanArrayBlockingQueue, batchSize, config);
            nettyClientList.add(nettyClient);
        }
    }

    private void initLoadBalance(Configuration configuration) {
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
        for (int i = 0; i < configuration.getInt(ConfigConstants.CLIENT_SEND_RETRIES_KEY, nettyClientList.size()); ++i) {
            NettyClient nettyClient = loadBalance.select(nettyClientList, span);
            if (nettyClient != null && nettyClient.isActive()) {
                nettyClient.write(span);
                return;
            }
        }
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
        for (NettyClient<BaseSpan> nettyClient : nettyClientList) {
            Closeables.close(nettyClient, true);
        }
    }

    static class BaseSpanNettyClient<T> extends NettyClient<T> implements Node {

        public BaseSpanNettyClient(BlockingQueue<T> queue, int batchSize, Config<T> config) {
            super(queue, batchSize, config);
        }

        @Override
        public String id() {
            return config.getServer();
        }
    }
}
