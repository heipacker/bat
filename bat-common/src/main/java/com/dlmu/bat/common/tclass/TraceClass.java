package com.dlmu.bat.common.tclass;

import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 代表一个需要trace的类
 */
public class TraceClass {

    private final Map<String, Set<TraceMethod>> desc2Method;
    private final String className;

    public TraceClass(String className) {
        this(className, new HashMap<String, Set<TraceMethod>>());
    }

    public TraceClass(String className, Map<String, Set<TraceMethod>> desc2Method) {
        this.className = className;
        this.desc2Method = desc2Method;
    }

    public void addMethod(TraceMethod method) {
        Set<TraceMethod> methods = desc2Method.get(method.getSignature());
        if (methods == null) {
            methods = new HashSet<TraceMethod>();
            desc2Method.put(method.getSignature(), methods);
        }
        methods.add(method);
    }

    public TraceMethod getMethod(String name, String desc, ProtectionDomain protectionDomain) {
        desc = internalDesc(desc);
        Set<TraceMethod> methods = desc2Method.get(name + desc);
        if (methods == null || methods.size() == 0) {
            return null;
        }
        for (TraceMethod method : methods) {
            if (method.match(protectionDomain)) {
                return method;
            }
        }
        return null;
    }

    private String internalDesc(String desc) {
        if (desc == null) return null;
        int index = desc.lastIndexOf(")");
        return desc.substring(0, index + 1);
    }

    public TraceClass merge(TraceClass newAdd) {
        Map<String, Set<TraceMethod>> temp = new HashMap<String, Set<TraceMethod>>(desc2Method);
        for (Map.Entry<String, Set<TraceMethod>> entry : newAdd.desc2Method.entrySet()) {
            Set<TraceMethod> methods = temp.get(entry.getKey());
            if (methods != null) {
                methods.addAll(entry.getValue());
            } else {
                methods = entry.getValue();
            }
            temp.put(entry.getKey(), methods);
        }
        return new TraceClass(newAdd.className, temp);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("className:");
        result.append(className).append("\n");
        for (Map.Entry<String, Set<TraceMethod>> entry : desc2Method.entrySet()) {
            result.append("methods:{");
            for (TraceMethod method : entry.getValue()) {
                result.append("method:{").append(method).append("}");
            }
            result.append("}");
        }
        return result.toString();
    }
}
