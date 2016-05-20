package com.dlmu.bat.common.tclass;

public class TraceField {
    public final String name;

    public final String desc;

    public final boolean isStatic;

    public TraceField(String name, String desc, boolean isStatic) {
        this.name = name;
        this.desc = desc;
        this.isStatic = isStatic;
    }

    @Override
    public String toString() {
        return "TraceField{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", isStatic=" + isStatic +
                '}';
    }
}
