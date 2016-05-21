package com.dlmu.bat.common.tclass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraceConfigParser {

    private static final Logger logger = LoggerFactory.getLogger(TraceConfigParser.class);

    public static final char SEPARATOR = ':';

    /**
     * @param lineReader
     * @return
     * @throws IOException
     */
    public Map<String, TraceClass> parse(BufferedReader lineReader) throws IOException {
        Map<String, TraceClass> traceClassMap = new HashMap<String, TraceClass>();
        String line;
        while ((line = lineReader.readLine()) != null) {
            try {
                StringReader reader = new StringReader(line);
                String className = internalName(readClassName(reader));
                if (className == null || className.length() == 0) {
                    continue;
                }
                TraceClass traceClass = traceClassMap.get(className);
                if (traceClass == null) {
                    traceClass = new TraceClass(className);
                    traceClassMap.put(className, traceClass);
                }
                TraceMethod method = readMethod(reader);
                if (method == null) {
                    continue;
                }
                traceClass.addMethod(method);
            } catch (Exception ignore) {
                logger.warn("process config error, skip {}", line);
            }
        }
        return traceClassMap;
    }

    /**
     * 将用户层面的class全名改到jvm层面的class
     *
     * @param name
     * @return
     */
    static String internalName(String name) {
        if (name == null) {
            return null;
        }
        return name.replace('.', '/');
    }

    /**
     * Two of the components of a method declaration comprise the method signature—the method's name and the parameter types.
     *
     * @param reader
     * @return
     * @throws IOException
     */
    private TraceMethod readMethod(StringReader reader) throws IOException {
        eatEmpty(reader);
        String signature = readUntil(reader, SEPARATOR);
        if (signature == null || signature.length() == 0) {
            return null;
        }

        StringReader signatureReader = new StringReader(signature);
        String name = readUntil(signatureReader, '(');
        List<TraceArg> args = readArgs(signatureReader);
        TraceMethod traceMethod = new TraceMethod(name, args);
        processFeatures(reader, traceMethod);
        return traceMethod;
    }

    private void processFeatures(StringReader reader, TraceMethod traceMethod) throws IOException {
        String feature = null;
        while (!(feature = readUntil(reader, SEPARATOR)).isEmpty()) {
            String[] arr = feature.split("=");
            if (arr.length != 2) {
                continue;
            }
            String key = arr[0];
            String value = arr[1];

            if (key == null || key.length() == 0) {
                continue;
            }

            if ("type".equals(key)) {
                traceMethod.setType(value);
            }

            if ("fields".equals(key)) {
                processFields(value, traceMethod);
            }

            if ("args".equals(key)) {
                String[] args = value.split(",");
                traceMethod.setTracedArgs(args);
            }

            if ("desc".equals(key)) {
                traceMethod.setDescription(value);
            }

            if (key.endsWith("alias")) {
                String name = key.substring(0, key.lastIndexOf('-'));
                traceMethod.addAlias(name, value);
            }

            if (key.endsWith("wrapper")) {
                String name = key.substring(0, key.lastIndexOf('-'));
                traceMethod.addWrapper(name, value);
            }

            if ("matcher".equals(key)) {
                traceMethod.setMatcher(value);
            }

        }
    }

    private void processFields(String fields, TraceMethod traceMethod) throws IOException {
        StringReader reader = new StringReader(fields);
        String field = null;
        while (!(field = readUntil(reader, ',')).isEmpty()) {
            traceMethod.addTracedField(readField(field));
        }
    }

    private TraceField readField(String field) throws IOException {
        StringReader reader = new StringReader(field);
        String name = readUntil(reader, '(');
        String desc = desc(readUntil(reader, ')'));
        boolean isStatic = reader.read() == 'S';
        return new TraceField(name, desc, isStatic);
    }

    private List<TraceArg> readArgs(StringReader reader) throws IOException {
        List<TraceArg> result = new ArrayList<TraceArg>();
        while (true) {
            String argType = desc(readToken(reader));
            if (argType == null || argType.length() == 0) {
                match(reader, ')');
                break;
            }
            String argName = readToken(reader);
            validateArg(argType, argName);
            result.add(new TraceArg(argType, argName));
            if (match(reader, ')')) {
                break;
            }
            match(reader, ',');
        }
        return result;
    }

    private void validateArg(String argType, String argName) {
        if (argType == null || argType.length() == 0
                || argName == null || argName.length() == 0)
            throw new IllegalArgumentException("");
    }

    private boolean match(StringReader reader, char expect) throws IOException {
        reader.mark(0);
        int c;
        while ((c = reader.read()) != -1) {
            if (c == expect) return true;
            if (c != ' ') {
                reader.reset();
                return false;
            }
            reader.mark(0);
        }
        return false;
    }

    private String readToken(StringReader reader) throws IOException {
        eatEmpty(reader);
        char c;
        StringBuilder temp = new StringBuilder();
        reader.mark(0);
        while ((c = (char) reader.read()) != -1) {
            if (c == '(' || c == ')' || c == ',' || c == ' ') {
                reader.reset();
                return temp.toString();
            }
            temp.append(c);
            reader.mark(0);
        }
        return temp.toString();
    }

    private String readClassName(StringReader reader) throws IOException {
        eatEmpty(reader);
        return readUntil(reader, SEPARATOR);
    }

    private void eatEmpty(StringReader reader) throws IOException {
        int c;
        reader.mark(0);
        while ((c = reader.read()) != -1) {
            if (c != ' ') {
                reader.reset();
                return;
            }
            reader.mark(0);
        }
    }

    private String readUntil(StringReader reader, char expected) throws IOException {
        StringBuilder result = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            if (c == expected) {
                break;
            }
            result.append((char) c);
        }
        return result.toString();
    }

    private String desc(String type) {
        if (type == null) {
            return null;
        }
        if (type.length() == 0) {
            return type;
        }

        StringBuilder result = new StringBuilder();
        while (type.endsWith("[]")) {
            result.append('[');
            type = type.substring(0, type.lastIndexOf('['));
        }
        if (type.equals("void")) {
            result.append('V');
        } else if (type.equals("boolean")) {
            result.append('Z');
        } else if (type.equals("byte")) {
            result.append('B');
        } else if (type.equals("char")) {
            result.append('C');
        } else if (type.equals("double")) {
            result.append('D');
        } else if (type.equals("float")) {
            result.append('F');
        } else if (type.equals("int")) {
            result.append('I');
        } else if (type.equals("long")) {
            result.append('J');
        } else if (type.equals("short")) {
            result.append('S');
        } else {
            result.append("L");
            appendClassDesc(type, result);
            result.append(';');
        }
        return result.toString();
    }

    private void appendClassDesc(String type, StringBuilder result) {
        int index = type.indexOf('<');
        if (index != -1) {
            type = type.substring(0, index);
        }
        result.append(type.replace('.', '/'));
    }
}
