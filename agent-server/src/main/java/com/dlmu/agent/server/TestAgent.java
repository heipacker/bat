package com.dlmu.agent.server;

import com.dlmu.agent.AgentInstaller;
import com.dlmu.agent.AgentSupport;

import java.lang.instrument.Instrumentation;

/**
 * Created by fupan on 16-4-2.
 */
public class TestAgent {

    /**
     * 第一个种， 程序启动是attach
     * VM options: -javaagent:/home/fupan/IdeaProjects/agentTest/agent-tools/target/agent-tools-1.0-SNAPSHOT.jar
     * 第二种， 程序启动后attach
     * attachAgent();
     *
     * @param args
     */
    public static void main(String[] args) {
        //程序启动时attach得把这个注释掉
        attachAgent();

        System.out.println("run test agent");
        Instrumentation instrumentation = AgentSupport.getInstrumentation();
        ClassTransformerSupport.addCode(instrumentation);
    }

    private static final String jarFilePath = "/home/fupan/IdeaProjects/agentTest/agent-tools/target/agent-tools-1.0-SNAPSHOT.jar";

    private static void attachAgent() {
        AgentSupport.loadAgent(jarFilePath);
    }
}
