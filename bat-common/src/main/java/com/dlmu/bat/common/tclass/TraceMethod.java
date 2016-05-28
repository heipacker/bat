package com.dlmu.bat.common.tclass;

import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.ProtectionDomain;
import java.util.*;

public class TraceMethod {
    private static final Logger logger = LoggerFactory.getLogger(TraceMethod.class);

    private String type = "BATTRACE";

    private String name;
    private List<TraceArg> args;
    private int[] tracedArgs;
    private List<TraceField> traceFields;
    private String desc;
    private Map<String, String> alias;
    private Map<String, Wrapper> wrappers;
    private String description;
    private Matcher matcher;
    private String watchId;
    private boolean traceReturnValue;

    private static final Map<String, Matcher> matchers = new HashMap<String, Matcher>();

    //send(java.lang.String subject);fields=host;args=subject;type=QMQ
    public TraceMethod(String name, List<TraceArg> args) {
        this.name = name;
        this.args = args;
        this.alias = new HashMap<String, String>();
        this.wrappers = new HashMap<String, Wrapper>();
        computeDesc();
    }

    private void computeDesc() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        int size = args.size();
        for (int i = 0; i < size; ++i) {
            String argDesc = args.get(i).desc;
            builder.append(argDesc);
        }

        builder.append(")");
        this.desc = builder.toString();
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTracedArgs(String[] tracedArgs) {
        if (args == null || args.size() == 0) return;
        int[] result = new int[args.size()];
        int k = 0;
        for (int i = 0; i < args.size(); ++i) {
            for (; k < tracedArgs.length; ) {
                if (!equal(args.get(i).name, tracedArgs[k])) break;
                k++;
                result[i] = 1;
            }
        }
        this.tracedArgs = result;
    }

    private static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public int[] getTracedArgs() {
        return tracedArgs;
    }

    public void addTracedField(TraceField traceField) {
        if (this.traceFields == null) {
            this.traceFields = new ArrayList<TraceField>();
        }
        this.traceFields.add(traceField);
    }

    public String getSignature() {
        return name + desc;
    }

    public String originalName() {
        return name;
    }

    public String newName() {
        return "$$dtrace$" + name.replace('<', '_').replace('>', '_') + "$generated";
    }

    public List<TraceField> getTracedFields() {
        return traceFields;
    }

    public String getType() {
        return type;
    }

    public void addAlias(String name, String alias) {
        this.alias.put(name, alias);
    }

    public String getAlias(String name) {
        String alias = this.alias.get(name);
        if (alias == null) return name;
        return alias;
    }

    public void addWrapper(String name, String wrapper) {
        this.wrappers.put(name, new Wrapper(wrapper));
    }

    public Wrapper getWrapper(String name) {
        return this.wrappers.get(name);
    }

    public List<TraceArg> getArgs() {
        return args;
    }

    public void setDescription(String description) {
        if (description != null && description.indexOf(TraceConfigParser.SEPARATOR) != -1) {
            this.description = description.replace(TraceConfigParser.SEPARATOR, '_');
        } else {
            this.description = description;
        }
    }

    public String getDescription() {
        if (description == null || description.length() == 0) {
            description = name + '(' + computeArgsDesc() + ')';
        }
        return description;
    }

    private String computeArgsDesc() {
        if (args == null) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < args.size(); ++i) {
            result.append(Type.getType(args.get(i).desc).getClassName()).append(' ').append(args.get(i).name);

            if (i < args.size() - 1) {
                result.append(',');
            }
        }
        return result.toString();
    }

    public boolean match(ProtectionDomain domain) {
        if (matcher == null) {
            return true;
        }
        return matcher.match(domain);
    }

    public void setMatcher(String value) {
        try {
            Matcher old = matchers.get(value);
            if (old != null) {
                this.matcher = old;
                return;
            }
            Class<?> matcherClass = Class.forName(value);
            this.matcher = (Matcher) matcherClass.newInstance();
            matchers.put(value, this.matcher);
        } catch (Throwable e) {
            logger.debug("set matcher failed", e);
        }
    }

    public String getWatchId() {
        return watchId;
    }

    public void setWatchId(String watchId) {
        this.watchId = watchId;
    }

    public boolean isTraceReturnValue() {
        return traceReturnValue;
    }

    public void setTraceReturnValue(boolean traceReturnValue) {
        this.traceReturnValue = traceReturnValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TraceMethod that = (TraceMethod) o;

        if (!getSignature().equals(that.getSignature())) return false;

        return matcher == that.matcher;
    }

    @Override
    public int hashCode() {
        int result = getSignature() != null ? getSignature().hashCode() : 0;
        result = 31 * result + (matcher != null ? matcher.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TraceMethod{" +
                "name='" + name + '\'' +
                ", args=" + args +
                ", type='" + type + '\'' +
                ", tracedArgs=" + Arrays.toString(tracedArgs) +
                ", traceFields=" + traceFields +
                ", desc='" + desc + '\'' +
                '}';
    }
}
