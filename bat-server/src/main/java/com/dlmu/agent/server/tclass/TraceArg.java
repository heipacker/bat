package com.dlmu.agent.server.tclass;

/**
 * Created by zhaohui.yu
 * 15/7/3
 */
public class TraceArg {
    public final String desc;

    public final String name;

    public TraceArg(String desc, String name) {
        this.desc = desc;
        this.name = name;
    }

    @Override
    public String toString() {
        return "TraceArg{" +
                "desc='" + desc + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
