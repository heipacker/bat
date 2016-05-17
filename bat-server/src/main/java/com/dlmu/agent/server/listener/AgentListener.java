package com.dlmu.agent.server.listener;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;

/**
 * Created by fupan on 16-4-10.
 */
public class AgentListener implements AgentBuilder.Listener {
    @Override
    public void onTransformation(TypeDescription typeDescription, DynamicType dynamicType) {
        System.out.println("onTransformation");
    }

    @Override
    public void onIgnored(TypeDescription typeDescription) {
        System.out.println("onIgnored");
    }

    @Override
    public void onError(String typeName, Throwable throwable) {
        System.out.println("onError");
    }

    @Override
    public void onComplete(String typeName) {
        System.out.println("onComplete");
    }
}
