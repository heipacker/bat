/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dlmu.bat.client.receiver;

import com.dlmu.bat.common.BaseSpan;
import com.dlmu.bat.common.OSUtils;
import com.dlmu.bat.common.metric.Metrics;
import com.dlmu.bat.plugin.conf.Configuration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.yammer.metrics.core.TimerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;

import static com.dlmu.bat.common.conf.ConfigConstants.*;

/**
 * Writes the spans it receives to a local file.
 */
public class LocalFileSpanReceiver extends SpanReceiver {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileSpanReceiver.class);

    private static ObjectWriter JSON_WRITER = new ObjectMapper().writer();
    private final String path;

    private byte[][] bufferedSpans;
    private int bufferedSpansIndex;
    private final ReentrantLock bufferLock = new ReentrantLock();

    private final FileOutputStream fileOutputStream;
    private final FileChannel channel;
    private final ReentrantLock channelLock = new ReentrantLock();

    public LocalFileSpanReceiver(Configuration configuration) {
        int capacity = configuration.getInt(RECEIVER_CAPACITY_KEY, DEFAULT_RECEIVER_CAPACITY);
        if (capacity < 1) {
            throw new IllegalArgumentException(RECEIVER_CAPACITY_KEY + " must not be less than 1.");
        }
        String pathStr = configuration.get(RECEIVER_LOCAL_FILE_PATH_KEY);
        if (pathStr == null || pathStr.isEmpty()) {
            path = getLocalTraceFileName();
        } else {
            path = pathStr;
        }
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
        this.bufferedSpans = new byte[capacity][];
        this.bufferedSpansIndex = 0;
        if (logger.isDebugEnabled()) {
            logger.debug("Created new LocalFileSpanReceiver with path = " + path + ", capacity = " + capacity);
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

    /**
     * Flushes a bufferedSpans array.
     */
    private void doFlush(byte[][] toFlush, int len) throws IOException {
        int bidx = 0, widx = 0;
        ByteBuffer writevBufs[] = new ByteBuffer[2 * WRITEV_SIZE];

        while (true) {
            if (widx == writevBufs.length) {
                channel.write(writevBufs);
                widx = 0;
            }
            if (bidx == len) {
                break;
            }
            writevBufs[widx] = ByteBuffer.wrap(toFlush[bidx]);
            writevBufs[widx + 1] = newlineBuf;
            bidx++;
            widx += 2;
        }
        if (widx > 0) {
            channel.write(writevBufs, 0, widx);
        }
    }

    @Override
    public void receiveSpan(BaseSpan span) {
        TimerContext context = Metrics.newTimer("receiveSpanTimer", Collections.<String, String>emptyMap()).time();
        try {
            // Serialize the span data into a byte[].  Note that we're not holding the lock here, to improve concurrency.
            byte jsonBuf[] = null;
            try {
                jsonBuf = JSON_WRITER.writeValueAsBytes(span);
            } catch (JsonProcessingException e) {
                logger.error("receiveSpan(path=" + path + ", span=" + span + "): Json processing error: " + e.getMessage());
                return;
            }

            // Grab the bufferLock and put our jsonBuf into the list of buffers to flush.
            byte toFlush[][] = null;
            bufferLock.lock();
            try {
                if (bufferedSpans == null) {
                    logger.debug("receiveSpan(path=" + path + ", span=" + span + "): LocalFileSpanReceiver for " + path + " is closed.");
                    return;
                }
                bufferedSpans[bufferedSpansIndex] = jsonBuf;
                bufferedSpansIndex++;
                if (bufferedSpansIndex == bufferedSpans.length) {
                    // If we've hit the limit for the number of buffers to flush,
                    // swap out the existing bufferedSpans array for a new array, and
                    // prepare to flush those spans to disk.
                    toFlush = bufferedSpans;
                    bufferedSpansIndex = 0;
                    bufferedSpans = new byte[bufferedSpans.length][];
                }
            } finally {
                bufferLock.unlock();
            }
            if (toFlush != null) {
                // We released the bufferLock above, to avoid blocking concurrent
                // receiveSpan calls.  But now, we must take the channelLock, to make
                // sure that we have sole access to the output channel.  If we did not do
                // this, we might get interleaved output.
                //
                // There is a small chance that another thread doing a flush of more
                // recent spans could get ahead of us here, and take the lock before we
                // do.  This is ok, since spans don't have to be written out in order.
                channelLock.lock();
                try {
                    doFlush(toFlush, toFlush.length);
                } catch (IOException ioe) {
                    logger.error("Error flushing buffers to " + path + ": " + ioe.getMessage());
                } finally {
                    channelLock.unlock();
                }
            }
        } finally {
            context.stop();
        }
    }

    @Override
    public void close() throws IOException {
        byte toFlush[][] = null;
        int numToFlush = 0;
        bufferLock.lock();
        try {
            if (bufferedSpans == null) {
                logger.info("LocalFileSpanReceiver for " + path + " was already closed.");
                return;
            }
            numToFlush = bufferedSpansIndex;
            bufferedSpansIndex = 0;
            toFlush = bufferedSpans;
            bufferedSpans = null;
        } finally {
            bufferLock.unlock();
        }
        channelLock.lock();
        try {
            doFlush(toFlush, numToFlush);
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

    private String getLocalTraceFileName() {
        String logDir = null;
        String basePath = System.getProperty("catalina.base");
        if (basePath != null) {
            File logDirFile = new File(basePath, "logs");
            if (logDirFile.exists()) {
                logDir = logDirFile.getAbsolutePath();
            }
        }
        if (logDir == null || !logDir.isEmpty()) {
            logDir = System.getProperty("java.io.tmpdir", "/tmp");
        }
        return new File(logDir, OSUtils.getOsPid() + ".trace.log").getAbsolutePath();
    }
}
