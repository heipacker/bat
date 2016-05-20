package com.dlmu.bat.common.tclass;

import org.objectweb.asm.Type;

public class Wrapper {

    public static final String WRAPPER_DESC = "(Ljava/lang/Object;)Ljava/lang/String;";

    public final String owner;
    public final String name;

    public Wrapper(String desc) {
        int index = desc.lastIndexOf('.');
        String clazzName = desc.substring(0, index);
        this.name = desc.substring(index + 1);
        try {
            Class<?> clazz = Class.forName(clazzName);
            this.owner = Type.getInternalName(clazz);
        } catch (Throwable e) {
            throw new RuntimeException("can not found class " + desc, e);
        }
    }

}
