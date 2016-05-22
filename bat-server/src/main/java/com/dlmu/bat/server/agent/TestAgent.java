package com.dlmu.bat.server.agent;

import com.dlmu.bat.agent.CosmosAgent;

import java.lang.instrument.Instrumentation;

/**
 * Created by fupan on 16-4-2.
 */
public class TestAgent {

    /**
     * 第一个种， 程序启动是attach
     * VM options: -javaagent:/home/fupan/IdeaProjects/bat/bat-agent/target/bat-agent-1.0-SNAPSHOT.jar
     * 第二种， 程序启动后attach
     * attachAgent();
     *
     * @param args
     */
    public static void main(String[] args) {
        //程序启动时attach得把这个注释掉
        CosmosAgent.install();

        System.out.println("run test agent");
        Instrumentation instrumentation = CosmosAgent.getInstrumentation();
        ClassTransformerSupport.addCode(instrumentation);
        new TestService().sayHello();
    }
}
