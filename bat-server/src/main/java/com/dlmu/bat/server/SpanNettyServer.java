package com.dlmu.bat.server;

import com.dlmu.bat.common.Constants;
import com.dlmu.bat.common.NetUtil;
import com.dlmu.bat.common.metric.Metrics;
import com.dlmu.bat.common.netty.NettyServer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher;

import java.util.Collections;
import java.util.List;

/**
 * Span Netty服务器。
 *
 * @author heipacker
 */
public class SpanNettyServer extends NettyServer {

    private final ZkClient zkClient;

    private final int bindPort;

    private volatile String node;

    private volatile Future<Void> future;

    public SpanNettyServer(ZkClient zkClient, List<NettyServer.Processor> processors, int bindPort) {
        super(1, Runtime.getRuntime().availableProcessors(), processors.toArray(new Processor[processors.size()]));
        this.zkClient = zkClient;
        this.bindPort = bindPort;
    }

    public void init() throws Exception {
        bind(bindPort).addListener(new GenericFutureListener<Future<Void>>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                logger.info("Server 启动成功");
                SpanNettyServer.this.future = future;
                register();
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        if (node != null) {
            zkClient.delete(node);
        }
        zkClient.close();
        super.destroy();
    }

    private void register() {
        doRegister();
        zkClient.subscribeStateChanges(new IZkStateListener() {
            @Override
            public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
                logger.warn("state change to {}", state);
            }

            @Override
            public void handleNewSession() throws Exception {
                doRegister();
            }

            @Override
            public void handleSessionEstablishmentError(Throwable error) throws Exception {
                logger.error("handleSessionEstablishmentError", error);
            }
        });
    }

    private void doRegister() {
        try {
            if (!zkClient.exists(Constants.SERVER_ROOT)) {
                zkClient.createPersistent(Constants.SERVER_ROOT);
            }
            String node = NetUtil.getLocalAddress().getHostAddress() + ":" + bindPort;
            node = ZKUtils.makePath(Constants.SERVER_ROOT, node);
            if (zkClient.exists(node)) {
                try {
                    zkClient.delete(node);
                } catch (Exception e) {
                    // ignore
                }
            }
            zkClient.createEphemeral(node);
            this.node = node;
            logger.info("ZK 注册成功, node {}", node);
        } catch (Exception e) {
            logger.error("注册服务失败", e);
        }
    }

    @Override
    protected void postProcessDecode(int readableBytes, String remoteInfo) {
        Metrics.newMeter("dtracer.spanDecodeSuccess", "", Collections.<String, String>emptyMap()).mark();
    }

    @Override
    protected void postProcessDecode(Throwable throwable, String remoteInfo) {

    }
}
