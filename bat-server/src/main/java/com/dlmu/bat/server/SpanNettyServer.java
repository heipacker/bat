package com.dlmu.bat.server;

import com.dlmu.bat.common.Constants;
import com.dlmu.bat.common.NetUtil;
import com.dlmu.bat.common.metric.Metrics;
import com.dlmu.bat.common.netty.NettyServer;
import com.dlmu.bat.common.register.RegistryUtils;
import com.dlmu.bat.server.zk.ConnectionState;
import com.dlmu.bat.server.zk.ConnectionStateListener;
import com.dlmu.bat.server.zk.ZKClient;
import com.dlmu.bat.server.zk.ZKClientCache;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.curator.utils.ZKPaths;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Span Netty服务器。
 *
 * @author heipacker
 */
public class SpanNettyServer extends NettyServer {

    private final ZKClient zkClient;

    private final int bindPort;

    private volatile String node;

    private volatile Future<Void> future;

    public SpanNettyServer(List<NettyServer.Processor> processors, int bindPort) {
        super(1, Runtime.getRuntime().availableProcessors(), processors.toArray(new Processor[processors.size()]));
        this.zkClient = ZKClientCache.get(RegistryUtils.resolve());
        this.bindPort = bindPort;
    }

    public void init() throws Exception {
        bind(bindPort).addListener(new GenericFutureListener<Future<Void>>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                logger.info("Server 启动成功");
                SpanNettyServer.this.future = future;
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        if (node != null) {
            zkClient.deletePath(node);
        }
        zkClient.close();
        super.destroy();
    }

    private void register() {
        doRegister();
        zkClient.addConnectionChangeListenter(new ConnectionStateListener() {
            @Override
            public void stateChanged(ZKClient sender, ConnectionState state) {
                if (state == ConnectionState.RECONNECTED) {
                    doRegister();
                }
            }
        });
    }

    private void doRegister() {
        try {
            if (!zkClient.checkExist(Constants.SERVER_ROOT)) {
                zkClient.addPersistentNode(Constants.SERVER_ROOT);
            }
            String node = NetUtil.getLocalAddress().getHostAddress() + ":" + bindPort;
            node = ZKPaths.makePath(Constants.SERVER_ROOT, node);
            if (zkClient.checkExist(node)) {
                try {
                    zkClient.deletePath(node);
                } catch (Exception e) {
                    // ignore
                }
            }
            zkClient.addEphemeralNode(node);
            this.node = node;
            logger.info("ZK 注册成功, node {}", node);
        } catch (Exception e) {
            logger.error("注册服务失败", e);
        }
    }

    @Override
    protected void postProcessDecode(int readableBytes, String remoteInfo) {
        Metrics.newMeter("dtracer.spanDecodeSuccess", "", TimeUnit.MILLISECONDS, Collections.<String, String>emptyMap()).mark();
    }

    @Override
    protected void postProcessDecode(Throwable throwable, String remoteInfo) {

    }
}
