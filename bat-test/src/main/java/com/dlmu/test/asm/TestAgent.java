package com.dlmu.test.asm;

import com.dlmu.agent.CosmosAgent;
import com.dlmu.agent.server.TestService;
import com.dlmu.bat.common.tclass.Configuration;
import com.dlmu.bat.common.transformer.TraceClassTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

/**
 * Created by fupan on 16-4-2.
 */
public class TestAgent {

    private static final Logger logger = LoggerFactory.getLogger(TestAgent.class);
    /**
     * 第一个种， 程序启动是attach
     * VM options: -javaagent:/home/fupan/IdeaProjects/bat/bat-tools/target/bat-tools-1.0-SNAPSHOT.jar
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
        Configuration configuration = new Configuration();
        if (!configuration.isInstrument()) {
            logger.warn("未开启instrument");
            return;
        }
        instrumentation.addTransformer(new TraceClassTransformer(configuration));
        new TestService().sayHello();
    }
}
