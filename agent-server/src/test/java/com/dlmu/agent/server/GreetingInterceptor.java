package com.dlmu.agent.server;

/**
 * Created by fupan on 16-4-9.
 */
public class GreetingInterceptor {
    public Object greet(Object argument) {
        return "Hello from " + argument;
    }
}