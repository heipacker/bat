package com.dlmu.bat.common.tclass;

import java.security.ProtectionDomain;

public interface Matcher {
    boolean match(ProtectionDomain domain);
}
