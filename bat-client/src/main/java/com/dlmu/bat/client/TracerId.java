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
package com.dlmu.bat.client;

import com.dlmu.bat.common.OSUtils;
import com.dlmu.bat.common.tname.Utils;
import com.dlmu.bat.plugin.conf.Configuration;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dlmu.bat.common.conf.ConfigConstants.DEFAULT_TRACER_ID;
import static com.dlmu.bat.common.conf.ConfigConstants.TRACER_ID_KEY;

/**
 * <p>The HTrace batClient ID.</p>
 * <p>
 * <p>HTrace batClient IDs are created from format strings.
 * Format strings contain variables which the TracerId class will
 * replace with the correct values at runtime.</p>
 * <p>
 * <ul>
 * <li>%{tname}: the batClient name supplied when creating the Tracer.</li>
 * <li>%{pname}: the process name obtained from the JVM.</li>
 * <li>%{ip}: will be replaced with an ip address.</li>
 * <li>%{pid}: the numerical process ID from the operating system.</li>
 * </ul>
 * <p>
 * <p>For example, the string "%{pname}/%{ip}" will be replaced with something
 * like: DataNode/192.168.0.1, assuming that the process' name is DataNode
 * and its IP address is 192.168.0.1.</p>
 * <p>
 * ID strings can contain backslashes as escapes.
 * For example, "\a" will map to "a".  "\%{ip}" will map to the literal
 * string "%{ip}", not the IP address.  A backslash itself can be escaped by a
 * preceding backslash.
 */
public final class TracerId {
    private static final Logger logger = LoggerFactory.getLogger(TracerId.class);

    private static final int[] codex = {2, 3, 5, 6, 8, 9, 19, 11, 12, 14, 15, 17, 18};
    private static final AtomicInteger messageOrder = new AtomicInteger(0);

    /**
     * 默认自动生成traceId
     *
     * @param conf
     * @param sample
     * @return
     */
    public static String next(Configuration conf, Sample sample) {
        if (conf != null) {
            String fmt = conf.get(TRACER_ID_KEY);
            if (!Strings.isNullOrEmpty(fmt)) {
                return next(conf);
            }
        }
        StringBuilder sb = new StringBuilder(40);
        long time = System.currentTimeMillis();
        String ts = new Timestamp(time).toString();

        for (int idx : codex)
            sb.append(ts.charAt(idx));
        sb.append('.').append(OSUtils.getBestIpString());
        sb.append('.').append(OSUtils.getOsPid());
        sb.append('.').append(messageOrder.getAndIncrement()); //可能为负数.但是无所谓.
        return Utils.getTName() + '_' + sb.toString() + "_" + sample.getSuffix();
    }

    /**
     * @param conf
     * @return
     */
    public static String next(Configuration conf) {
        String fmt = conf.get(TRACER_ID_KEY, DEFAULT_TRACER_ID);
        StringBuilder bld = new StringBuilder();
        StringBuilder varBld = null;
        boolean escaping = false;
        int varSeen = 0;
        for (int i = 0, len = fmt.length(); i < len; i++) {
            char c = fmt.charAt(i);
            if (c == '\\') {
                if (!escaping) {
                    escaping = true;
                    continue;
                }
            }
            switch (varSeen) {
                case 0:
                    if (c == '%') {
                        if (!escaping) {
                            varSeen = 1;
                            continue;
                        }
                    }
                    escaping = false;
                    varSeen = 0;
                    bld.append(c);
                    break;
                case 1:
                    if (c == '{') {
                        if (!escaping) {
                            varSeen = 2;
                            varBld = new StringBuilder();
                            continue;
                        }
                    }
                    escaping = false;
                    varSeen = 0;
                    bld.append("%").append(c);
                    break;
                default:
                    if (c == '}') {
                        if (!escaping) {
                            String var = varBld.toString();
                            bld.append(processShellVar(var));
                            varBld = null;
                            varSeen = 0;
                            continue;
                        }
                    }
                    escaping = false;
                    varBld.append(c);
                    varSeen++;
                    break;
            }
        }
        if (varSeen > 0) {
            logger.warn("Unterminated process ID substitution variable at the end " +
                    "of format string " + fmt);
        }
        String traceId = bld.toString();
        if (logger.isTraceEnabled()) {
            logger.trace("ProcessID(fmt=" + fmt + "): computed process ID of \"" +
                    traceId + "\"");
        }
        return traceId;
    }

    private static String processShellVar(String var) {
        if (var.equals("tname")) {//将appName修改为traceName 抽象一下
            return Utils.getTName();
        } else if (var.equals("pname")) {
            return OSUtils.getProcessName();
        } else if (var.equals("ip")) {
            return OSUtils.getBestIpString();
        } else if (var.equals("pid")) {
            return Long.valueOf(OSUtils.getOsPid()).toString();
        } else {
            logger.warn("unknown ProcessID variable " + var);
            return "";
        }
    }

}
