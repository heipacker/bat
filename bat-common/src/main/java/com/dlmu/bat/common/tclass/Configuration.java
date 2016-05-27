package com.dlmu.bat.common.tclass;

import com.dlmu.bat.common.tname.Utils;
import com.dlmu.bat.common.transformer.DTraceGenerated;
import com.dlmu.bat.common.Constants;
import com.dlmu.bat.common.conf.DTraceConfiguration;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private volatile Map<String, TraceClass> config;

    private DTraceConfiguration conf;

    private TraceConfigParser parser;

    private final AtomicBoolean loaded = new AtomicBoolean(false);

    private static final Object HOLDER = new Object();
    private final ConcurrentMap<String, Object> watches;

    public Configuration() {
        this.config = new HashMap<String, TraceClass>();
        this.parser = new TraceConfigParser();
        this.watches = new ConcurrentHashMap<String, Object>();

        loadLocalConfig();
        Map<String, String> globalConfig = loadGlobalConfig("instrument.properties");
        conf = DTraceConfiguration.fromMap(globalConfig);

        String instrumentMethodContent = loadCommonInstrumentMethod("instrument-method");
        if (loaded.compareAndSet(false, true)) {
            try {
                merge(parser.parse(new BufferedReader(new StringReader(instrumentMethodContent))));
            } catch (IOException e) {
                logger.error("parse instrument method error", e);
            }
        }
    }

    private String loadCommonInstrumentMethod(String fileName) {
        return "";
    }

    private Map<String, String> loadGlobalConfig(String fileName) {
        //// TODO: 16-5-18 添加获取global配置
        HashMap<String, String> globalConfig = new HashMap<String, String>();
        globalConfig.put("default", "true");

        return globalConfig;
    }

    private void loadLocalConfig() {
        try {
            Enumeration<URL> resources = Configuration.class.getClassLoader().getResources(Constants.DTRACER_CONFIG_FILE);
            if (resources == null) {
                logger.warn("can not read config file by default loader");
                resources = ClassLoader.getSystemResources(Constants.DTRACER_CONFIG_FILE);
            }
            if (resources == null) {
                logger.warn("can not read config file by system loader");
                return;
            }

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                Map<String, TraceClass> result = parser.parse(reader);
                merge(result);
            }
        } catch (IOException ignore) {

        }

    }

    private void merge(Map<String, TraceClass> result) {
        if (this.config == null || this.config.size() == 0) {
            this.config = result;
            return;
        }
        Map<String, TraceClass> temp = new HashMap<String, TraceClass>(this.config);
        for (Map.Entry<String, TraceClass> entry : result.entrySet()) {
            TraceClass origin = temp.get(entry.getKey());
            if (origin == null) {
                temp.put(entry.getKey(), entry.getValue());
            } else {
                temp.put(entry.getKey(), origin.merge(entry.getValue()));
            }
        }
        this.config = temp;
    }

    public boolean isInstrument() {
        boolean defaultSwitch = conf.getBoolean("default", false);
        return getBoolean(Utils.getTName(), defaultSwitch);
    }

    private boolean getBoolean(String name, boolean def) {
        String value = conf.get(name);
        if (value == null || value.length() == 0) {
            return def;
        }
        return getBoolean(value);
    }

    private boolean getBoolean(String value) {
        value = value.trim().toUpperCase();
        return "TRUE".equals(value) || "YES".equals(value) || "ON".equals(value) || "1".equals(value);
    }


    public TraceClass match(String className) {
        return config.remove(className);
    }

    public boolean addWatch(String watchId, Class clazz, String methodName) {
        String className = TraceConfigParser.internalName(clazz.getCanonicalName());
        if (!canWatch(className)) {
            return false;
        }
        Method[] methods = clazz.getDeclaredMethods();
        TraceClass traceClass = new TraceClass(className);
        for (Method method : methods) {
            if (needSkip(method)) continue;

            if (!match(method, methodName)) continue;

            Class<?>[] parameterTypes = method.getParameterTypes();
            List<TraceArg> args = new ArrayList<TraceArg>(parameterTypes.length);
            String[] tracedArgs = new String[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; ++i) {
                String name = "arg" + i;
                tracedArgs[i] = name;
                TraceArg traceArg = new TraceArg(Type.getDescriptor(parameterTypes[i]), name);
                args.add(traceArg);
            }
            TraceMethod traceMethod = new TraceMethod(method.getName(), args);
            traceMethod.setType("WATCH");
            traceMethod.setTracedArgs(tracedArgs);
            traceMethod.setWatchId(watchId);
            traceMethod.setTraceReturnValue(true);
            traceClass.addMethod(traceMethod);
        }
        Map<String, TraceClass> map = new HashMap<String, TraceClass>();
        map.put(className, traceClass);
        merge(map);
        return true;
    }

    private boolean canWatch(String className) {
        return watches.putIfAbsent(className, HOLDER) == null;
    }

    private boolean needSkip(Method method) {
        int modifiers = method.getModifiers();
        if (Modifier.isAbstract(modifiers)) {
            return true;
        }
        if (Modifier.isNative(modifiers)) {
            return true;
        }
        if (method.isBridge()) {
            return true;
        }
        if (method.isSynthetic()) {
            return true;
        }
        if (exist(method, DTraceGenerated.class)) {
            return true;
        }
        return false;
    }

    private boolean match(Method method, String methodName) {
        if (methodName == null || methodName.length() == 0) {
            return true;
        }
        return method.getName().equals(methodName);
    }

    private boolean exist(Method method, Class clazz) {
        Annotation annotation = method.getAnnotation(clazz);
        return annotation != null;
    }
}
