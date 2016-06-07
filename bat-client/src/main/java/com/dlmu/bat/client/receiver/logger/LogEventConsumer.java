package com.dlmu.bat.client.receiver.logger;

import com.google.common.base.Charsets;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author heipacker
 * @date 16-5-29.
 */
public class LogEventConsumer implements EventHandler<LogEvent>, Closeable {

    private static final Logger logger = LoggerFactory.getLogger(LogEventConsumer.class);

    private String path;

    private final FileOutputStream fileOutputStream;
    private final FileChannel channel;

    private static final ReentrantLock channelLock = new ReentrantLock();

    private BlockingQueue<LogEvent> batchList = new ArrayBlockingQueue<LogEvent>(100);

    public LogEventConsumer(String path) {
        this.path = path;
        boolean success = false;
        try {
            this.fileOutputStream = new FileOutputStream(path, true);
        } catch (IOException ioe) {
            logger.error("Error opening " + path + ": " + ioe.getMessage());
            throw new RuntimeException(ioe);
        }
        this.channel = fileOutputStream.getChannel();
        if (this.channel == null) {
            try {
                this.fileOutputStream.close();
            } catch (IOException e) {
                logger.error("Error closing " + path, e);
            }
            logger.error("Failed to get channel for " + path);
            throw new RuntimeException("Failed to get channel for " + path);
        }
    }

    @Override
    public void onEvent(LogEvent event, long sequence, boolean endOfBatch) throws Exception {
        batchList.offer(event);
        if (endOfBatch) {
            channelLock.lock();
            try {
                doFlush();
            } finally {
                channelLock.unlock();
            }
        }
    }

    /**
     * Number of buffers to use in FileChannel#write.
     * <p>
     * On UNIX, FileChannel#write uses writev-- a kernel interface that allows
     * us to send multiple buffers at once.  This is more efficient than making a
     * separate write call for each buffer, since it minimizes the number of
     * transitions from userspace to kernel space.
     */
    private final int WRITEV_SIZE = 20;

    private final static ByteBuffer newlineBuf = ByteBuffer.wrap(new byte[]{(byte) 0xa});

    private void doFlush() throws IOException {
        int widx = 0;
        ByteBuffer[] writevBufs = new ByteBuffer[2 * WRITEV_SIZE];

        LogEvent logEvent;
        while ((logEvent = batchList.poll()) != null) {
            if (widx == writevBufs.length) {
                channel.write(writevBufs);
                widx = 0;
            }
            String logEventLog = logEvent.getLog();
            if (logEventLog == null) {
                continue;
            }
            writevBufs[widx] = ByteBuffer.wrap(logEventLog.getBytes(Charsets.UTF_8));
            writevBufs[widx + 1] = newlineBuf;
            widx += 2;
        }
        if (widx > 0) {
            channel.write(writevBufs, 0, widx);
        }
    }

    public void close() {
        channelLock.lock();
        try {
            doFlush();
        } catch (IOException ioe) {
            logger.error("Error flushing buffers to " + path + ": " + ioe.getMessage());
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                logger.error("Error closing fileOutputStream for " + path, e);
            }
            channelLock.unlock();
        }
    }
}
