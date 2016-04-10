package com.dlmu.agent;

import java.lang.instrument.Instrumentation;

/**
 * Created by fupan on 16-4-2.
 */
public class AgentInstaller {

    public static Instrumentation instrumentation = null;

    /**
     * 命令行启动
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("pre main running");
        AgentInstaller.instrumentation = instrumentation;
    }

    /**
     * 命令行启动
     */
    public static void premain(String agentArgs) {
        System.out.println("pre main running");
    }

    /**
     * 类加载调用
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("agent main running");
        AgentInstaller.instrumentation = instrumentation;
    }

    public static Instrumentation instrumentation() {
        return instrumentation;
    }
}
