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
package com.dlmu.bat.plugin.conf.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This is an implementation of HTraceConfiguration which draws its properties
 * from global Java Properties.
 */
public final class JavaPropertyConfiguration extends AbstractConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(JavaPropertyConfiguration.class);

    private String[] prefixes;

    public JavaPropertyConfiguration() {
        prefixes = new String[]{""};
    }

    private JavaPropertyConfiguration(LinkedList<String> prefixes) {
        this.prefixes = new String[prefixes.size()];
        int i = 0;
        for (Iterator<String> it = prefixes.descendingIterator(); it.hasNext(); ) {
            this.prefixes[i++] = it.next();
        }
    }

    @Override
    public void put(String key, String value) {
        for (String prefix : prefixes) {
            if (key.startsWith(prefix)) {
                System.setProperty(key.substring(prefix.length()), value);
                break;
            }
        }
    }

    @Override
    public int index() {
        return 299;
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        Map<String, String> result = new HashMap<String, String>();
        Properties properties = System.getProperties();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = String.valueOf(entry.getKey());
            for (String prefix : prefixes) {
                if (key.startsWith(prefix)) {
                    result.put(key.substring(prefix.length()), String.valueOf(entry.getValue()));
                    break;
                }
            }
        }
        return result.entrySet().iterator();
    }

    @Override
    public String get(String key) {
        for (String prefix : prefixes) {
            String val = System.getProperty(prefix + key);
            if (val != null) {
                return val;
            }
        }
        return null;
    }

    @Override
    public String get(String key, String defaultValue) {
        String val = get(key);
        return (val != null) ? val : defaultValue;
    }
}
