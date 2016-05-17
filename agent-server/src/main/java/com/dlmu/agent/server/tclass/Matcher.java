package com.dlmu.agent.server.tclass;

import java.security.ProtectionDomain;

public interface Matcher {
    boolean match(ProtectionDomain domain);
}
