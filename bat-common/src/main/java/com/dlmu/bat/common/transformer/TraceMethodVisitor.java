package com.dlmu.bat.common.transformer;

import com.dlmu.bat.common.Constants;
import com.dlmu.bat.common.tclass.TraceArg;
import com.dlmu.bat.common.tclass.TraceField;
import com.dlmu.bat.common.tclass.TraceMethod;
import com.dlmu.bat.common.tclass.Wrapper;
import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TraceMethodVisitor extends MethodVisitor implements Opcodes {

    private static final String TRACE_GENERATED_DESC = Type.getDescriptor(BatTraceGenerated.class);

    private static final String TRACEUTILS_INTERNALNAME = "com/dlmu/bat/client/TraceUtils";

    private static final String STRINGBUILDER_INTERNALNAME = "java/lang/StringBuilder";

    private static final String ADDKVANNOTATION_DESC = "(Lcom/dlmu/bat/client/TraceScope;Ljava/lang/String;Ljava/lang/Object;)V";

    private static final Map<String, String> ADDKVANNOTATION_DESC_MAP = new HashMap<String, String>();

    public static final String TRACE_ADD_KV_ANNOTATION = "addKVAnnotation";

    static {
        ADDKVANNOTATION_DESC_MAP.put("Z", "(Lcom/dlmu/bat/client/TraceScope;Ljava/lang/String;Z)V");
        ADDKVANNOTATION_DESC_MAP.put("B", "(Lcom/dlmu/bat/client/TraceScope;Ljava/lang/String;B)V");
        ADDKVANNOTATION_DESC_MAP.put("C", "(Lcom/dlmu/bat/client/TraceScope;Ljava/lang/String;C)V");
        ADDKVANNOTATION_DESC_MAP.put("D", "(Lcom/dlmu/bat/client/TraceScope;Ljava/lang/String;D)V");
        ADDKVANNOTATION_DESC_MAP.put("F", "(Lcom/dlmu/bat/client/TraceScope;Ljava/lang/String;F)V");
        ADDKVANNOTATION_DESC_MAP.put("I", "(Lcom/dlmu/bat/client/TraceScope;Ljava/lang/String;I)V");
        ADDKVANNOTATION_DESC_MAP.put("J", "(Lcom/dlmu/bat/client/TraceScope;Ljava/lang/String;J)V");
        ADDKVANNOTATION_DESC_MAP.put("S", "(Lcom/dlmu/bat/client/TraceScope;Ljava/lang/String;S)V");
    }

    private static final String APPEND_NAME = "append";

    private static final String QTRACE_SCOPE_INTERNALNAME = "com/dlmu/bat/client/TraceScope";

    private final TraceMethod method;

    private final MethodVisitor traceMethod;
    private final int newExceptionsLen;
    private final String[] newMethodExceptions;
    private final Type[] parameterTypes;
    private final Type returnType;
    private final boolean hasReturn;
    private final String desc;
    private final String className;

    private final int totalParameterSize;
    private final int startOfVarIndex;

    public TraceMethodVisitor(int access, String desc, String signature, String[] exceptions, String className, TraceMethod method, ClassVisitor cv) {
        super(ASM5, cv.visitMethod(Access.of(access).remove(ACC_PUBLIC).remove(ACC_PROTECTED).remove(ACC_SYNCHRONIZED).add(ACC_PRIVATE).add(ACC_FINAL).get(), method.newName(), desc, signature, exceptions));
        this.className = className;
        this.parameterTypes = Type.getArgumentTypes(desc);
        this.returnType = Type.getReturnType(desc);
        this.hasReturn = this.returnType != Type.VOID_TYPE;
        this.desc = desc;
        this.method = method;

        this.totalParameterSize = computeTotalParameterSize(parameterTypes);
        this.startOfVarIndex = Access.of(access).contain(Opcodes.ACC_STATIC) ? 0 : 1;

        newMethodExceptions = exceptions == null ? new String[]{"java/lang/RuntimeException"} : exceptions;
        newExceptionsLen = newMethodExceptions.length;
        traceMethod = cv.visitMethod(access, method.originalName(), desc, signature, newMethodExceptions);
        addGeneratedAnnotation();
    }

    private void addGeneratedAnnotation() {
        AnnotationVisitor av = traceMethod.visitAnnotation(TRACE_GENERATED_DESC, true);
        av.visitEnd();
        av = mv.visitAnnotation(TRACE_GENERATED_DESC, true);
        av.visitEnd();
    }

    @Override
    public void visitParameter(String name, int access) {
        super.visitParameter(name, access);
        traceMethod.visitParameter(name, access);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return traceMethod.visitAnnotationDefault();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return traceMethod.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        return traceMethod.visitTypeAnnotation(typeRef, typePath, desc, visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        return traceMethod.visitParameterAnnotation(parameter, desc, visible);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
    }

    /**
     * 将原来的方法重命名为一个private方法，然后在原来未改名的方法里调用原来的方法
     */
    @Override
    public void visitCode() {
        super.visitCode();
        traceMethod.visitCode();

        int scopeVarIndex = startOfVarIndex + totalParameterSize;

        Label startOfTryCatch = new Label();
        Label endOfTryCatch = new Label();
        Label[] exceptionHandlers = new Label[newExceptionsLen];

        //catchs
        for (int i = 0, length = exceptionHandlers.length; i < length; i++) {
            traceMethod.visitTryCatchBlock(startOfTryCatch, endOfTryCatch, exceptionHandlers[i] = new Label(), newMethodExceptions[i]);
        }

        //finally
        Label endOfFinally = new Label();
        Label handlerOfFinally = new Label();
        traceMethod.visitTryCatchBlock(startOfTryCatch, endOfTryCatch, handlerOfFinally, null);
        traceMethod.visitTryCatchBlock(exceptionHandlers[0], endOfFinally, handlerOfFinally, null);

        startTrace(scopeVarIndex);

        attachWatchId(scopeVarIndex);
        attachFields(scopeVarIndex);
        attachArgs(scopeVarIndex);

        int returnVarIndex = scopeVarIndex + 1;

        //try{
        //call original method
        //}catch(...){
        traceMethod.visitLabel(startOfTryCatch);
        callOriginal(returnVarIndex, startOfVarIndex, traceMethod);
        traceMethod.visitLabel(endOfTryCatch);

        attachReturnValue(scopeVarIndex, returnVarIndex);

        endTrace(scopeVarIndex);

        Label end = null;
        if (!hasReturn) {
            end = new Label();
            traceMethod.visitJumpInsn(GOTO, end);
        } else {
            traceMethod.visitVarInsn(returnType.getOpcode(ILOAD), returnVarIndex);
            traceMethod.visitInsn(returnType.getOpcode(IRETURN));
        }

        emitCatchBlocks(scopeVarIndex, exceptionHandlers);

        traceMethod.visitLabel(handlerOfFinally);
        traceMethod.visitVarInsn(ASTORE, returnVarIndex);
        //finally
        traceMethod.visitLabel(endOfFinally);

        endTrace(scopeVarIndex);

        traceMethod.visitVarInsn(ALOAD, returnVarIndex);
        traceMethod.visitInsn(ATHROW);

        if (!hasReturn) {
            traceMethod.visitLabel(end);
            traceMethod.visitInsn(RETURN);
        }
    }

    private void attachWatchId(int scopeVarIndex) {
        String watchId = method.getWatchId();
        if (watchId == null) return;

        emitAddKVAnnotation(scopeVarIndex, Constants.TRACE_WATCHID, watchId);
    }

    private void attachReturnValue(int scopeVarIndex, int returnVarIndex) {
        if (!this.method.isTraceReturnValue()) return;
        if (!hasReturn) return;

        traceMethod.visitVarInsn(ALOAD, scopeVarIndex);
        traceMethod.visitLdcInsn("return");
        traceMethod.visitVarInsn(returnType.getOpcode(ILOAD), returnVarIndex);
        invokeAddKVAnnotation(returnType.getDescriptor());
    }

    private void emitCatchBlocks(int scopeVarIndex, Label[] exceptionHandlers) {
        //catch blocks
        for (int i = 0; i < newExceptionsLen; i++) {
            traceMethod.visitLabel(exceptionHandlers[i]);
            //ex
            int exceptionVarIndex = scopeVarIndex + 1;
            traceMethod.visitVarInsn(ASTORE, exceptionVarIndex);

            //StringBuilder message = new StringBuilder();
            //message.append("type:");
            //message.append(ex.getClass());
            //message.append(",message:");
            //message.append(ex.getMessage());
            //scope.addAnnotation(Constants.EXCEPTION_KEY,message.toString());
            traceMethod.visitVarInsn(ALOAD, scopeVarIndex);
            traceMethod.visitLdcInsn(Constants.EXCEPTION_KEY);

            traceMethod.visitTypeInsn(NEW, STRINGBUILDER_INTERNALNAME);
            traceMethod.visitInsn(DUP);
            traceMethod.visitMethodInsn(INVOKESPECIAL, STRINGBUILDER_INTERNALNAME, "<init>", "()V", false);
            traceMethod.visitLdcInsn("type:");
            traceMethod.visitMethodInsn(INVOKEVIRTUAL, STRINGBUILDER_INTERNALNAME, APPEND_NAME, "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

            traceMethod.visitVarInsn(ALOAD, exceptionVarIndex);
            traceMethod.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            traceMethod.visitMethodInsn(INVOKEVIRTUAL, STRINGBUILDER_INTERNALNAME, APPEND_NAME, "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);

            traceMethod.visitLdcInsn(",message:");
            traceMethod.visitMethodInsn(INVOKEVIRTUAL, STRINGBUILDER_INTERNALNAME, APPEND_NAME, "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

            traceMethod.visitVarInsn(ALOAD, exceptionVarIndex);
            traceMethod.visitMethodInsn(INVOKEVIRTUAL, newMethodExceptions[i], "getMessage", "()Ljava/lang/String;", false);
            traceMethod.visitMethodInsn(INVOKEVIRTUAL, STRINGBUILDER_INTERNALNAME, APPEND_NAME, "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

            traceMethod.visitMethodInsn(INVOKEVIRTUAL, STRINGBUILDER_INTERNALNAME, "toString", "()Ljava/lang/String;", false);
            traceMethod.visitMethodInsn(INVOKEINTERFACE, QTRACE_SCOPE_INTERNALNAME, TRACE_ADD_KV_ANNOTATION, "(Ljava/lang/String;Ljava/lang/String;)V", true);

            //scope.addKVAnnotation(Constants.QTRACE_STATUS,Constants.QTRACE_STATUS_ERROR);
            emitAddKVAnnotation(scopeVarIndex, Constants.TRACE_STATUS, Constants.TRACE_STATUS_ERROR);

            //throw ex
            traceMethod.visitVarInsn(ALOAD, exceptionVarIndex);
            traceMethod.visitInsn(ATHROW);
        }
    }

    private void endTrace(int scopeVarIndex) {
        traceMethod.visitVarInsn(ALOAD, scopeVarIndex);
        traceMethod.visitMethodInsn(INVOKEINTERFACE, QTRACE_SCOPE_INTERNALNAME, "close", "()V", true);
    }

    private void startTrace(int scopeVarIndex) {
        //BatClient client = BatClientGetter.getClient();
        traceMethod.visitMethodInsn(INVOKESTATIC, "com/dlmu/bat/client/BatClientGetter", "getClient", "()Lcom/dlmu/bat/client/BatClient;", false);
        // TraceScope scope = client.newScope(method.getSignature(), traceInfo);
        traceMethod.visitLdcInsn(method.getDescription());
        traceMethod.visitFieldInsn(GETSTATIC, "com/dlmu/bat/client/TraceUtils", "NEW_NO_TRACE", "Lcom/dlmu/bat/client/TraceInfo;");
        traceMethod.visitMethodInsn(INVOKEINTERFACE, "com/dlmu/bat/client/BatClient", "newScope", "(Ljava/lang/String;Lcom/dlmu/bat/client/TraceInfo;)Lcom/dlmu/bat/client/TraceScope;", true);
        //store scope to local variable
        traceMethod.visitVarInsn(ASTORE, scopeVarIndex);

        //scope.addKVAnnotation(Constants.TRACE_TYPE,method.getType);
        emitAddKVAnnotation(scopeVarIndex, Constants.TRACE_TYPE, method.getType());
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack, maxLocals);
        int computeMaxLocals = computeMaxLocals();
        traceMethod.visitMaxs(Math.max(maxStack, 4), computeMaxLocals);
    }

    /**
     * @return this(1) + parameters size(n * per size) + scope(1) + hasReturn ? return size : exception(1)
     */
    private int computeMaxLocals() {
        return startOfVarIndex + totalParameterSize + 1 + (hasReturn ? returnType.getSize() : 1);
    }

    private void attachFields(int scopeVariableIndex) {
        List<TraceField> fields = method.getTracedFields();
        if (fields == null || fields.size() == 0) return;
        for (int i = 0; i < fields.size(); ++i) {
            TraceField field = fields.get(i);
            traceMethod.visitVarInsn(ALOAD, scopeVariableIndex);
            traceMethod.visitLdcInsn(this.method.getAlias(field.name));
            if (!field.isStatic) {
                traceMethod.visitVarInsn(ALOAD, 0);
                traceMethod.visitFieldInsn(GETFIELD, this.className, field.name, field.desc);
            } else {
                traceMethod.visitFieldInsn(GETSTATIC, this.className, field.name, field.desc);
            }
            wrappedIfNecessary(field.desc, field.name);
            invokeAddKVAnnotation(field.desc);
        }
    }

    private void attachArgs(int scopeVarIndex) {
        List<TraceArg> args = method.getArgs();
        if (args == null || args.size() == 0) return;

        int[] tracedArgs = method.getTracedArgs();
        if (tracedArgs == null || tracedArgs.length == 0) return;
        if (parameterTypes.length != args.size()) return;
        if (parameterTypes.length != tracedArgs.length) return;
        int index = this.startOfVarIndex;
        for (int i = 0; i < parameterTypes.length; ++i) {
            Type parameterType = parameterTypes[i];
            if (tracedArgs[i] == 1) {
                TraceArg traceArg = args.get(i);
                traceMethod.visitVarInsn(ALOAD, scopeVarIndex);
                traceMethod.visitLdcInsn(traceArg.name);
                traceMethod.visitVarInsn(parameterType.getOpcode(ILOAD), index);
                wrappedIfNecessary(parameterType.getDescriptor(), traceArg.name);
                invokeAddKVAnnotation(parameterType.getDescriptor());
            }
            index += parameterType.getSize();
        }
    }

    private void invokeAddKVAnnotation(String desc) {
        String addKVAnnotationDesc = ADDKVANNOTATION_DESC_MAP.get(desc);
        if (addKVAnnotationDesc == null) addKVAnnotationDesc = ADDKVANNOTATION_DESC;
        traceMethod.visitMethodInsn(INVOKESTATIC, TRACEUTILS_INTERNALNAME, TRACE_ADD_KV_ANNOTATION, addKVAnnotationDesc, false);
    }

    private void wrappedIfNecessary(String desc, String name) {
        if (isPrimitive(desc)) return;
        Wrapper wrapper = this.method.getWrapper(name);
        if (wrapper == null) return;
        traceMethod.visitMethodInsn(INVOKESTATIC, wrapper.owner, wrapper.name, Wrapper.WRAPPER_DESC, false);
    }

    private boolean isPrimitive(String desc) {
        return (desc.equals("Z") || desc.equals("B")
                || desc.equals("C") || desc.equals("D")
                || desc.equals("F") || desc.equals("F")
                || desc.equals("I") || desc.equals("J")
                || desc.equals("S"));
    }

    private void emitAddKVAnnotation(int scopeVariableIndex, String key, String value) {
        traceMethod.visitVarInsn(ALOAD, scopeVariableIndex);
        traceMethod.visitLdcInsn(key);
        traceMethod.visitLdcInsn(value);
        traceMethod.visitMethodInsn(INVOKEINTERFACE, QTRACE_SCOPE_INTERNALNAME, TRACE_ADD_KV_ANNOTATION, "(Ljava/lang/String;Ljava/lang/String;)V", true);
    }

    private void callOriginal(int returnVarIndex, int defaultSize, MethodVisitor traceMethod) {
        //this
        if (defaultSize == 1) {
            traceMethod.visitVarInsn(ALOAD, 0);
        }
        //load parameters to stack
        for (int i = 0, index = 0, preSize = defaultSize; i < parameterTypes.length; i++) {
            index += preSize;
            Type parameterType = parameterTypes[i];
            traceMethod.visitVarInsn(parameterType.getOpcode(ILOAD), index);
            preSize = parameterType.getSize();
        }
        traceMethod.visitMethodInsn(defaultSize == 1 ? INVOKEVIRTUAL : INVOKESTATIC, this.className, method.newName(), desc, false);
        if (hasReturn) {
            traceMethod.visitVarInsn(returnType.getOpcode(ISTORE), returnVarIndex);
        }
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        traceMethod.visitEnd();
    }

    private int computeTotalParameterSize(Type[] parameterTypes) {
        int result = 0;
        int parameterCount = parameterTypes.length;
        for (int i = 0; i < parameterCount; i++) {
            result += parameterTypes[i].getSize();
        }
        return result;
    }
}
