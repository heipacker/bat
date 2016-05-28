package com.dlmu.test.asm;

import com.dlmu.bat.agent.CosmosAgent;
import com.dlmu.bat.client.DTraceClient;
import com.dlmu.bat.client.DTraceClientGetter;
import com.dlmu.bat.common.tclass.Conf;
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
        Conf configuration = new Conf();
        if (!configuration.isInstrument()) {
            logger.warn("未开启instrument");
            return;
        }
        instrumentation.addTransformer(new TraceClassTransformer(configuration));
        new TestService().sayHello();
    }
}
