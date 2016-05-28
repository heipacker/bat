package com.dlmu.bat.client;

import com.google.common.base.Optional;

/**
 * @author heipacker
 * @date 16-5-23.
 */
public enum Sample {
    /**
     * 必须采样,不能被覆盖
     */
    MUST('9'),

    /**
     * 应该采样，但是可以被覆盖
     */
    SHOULD('1'),

    /**
     * 不采样
     */
    NO('0');

    private char suffix;

    Sample(char suffix) {
        this.suffix = suffix;
    }

    public char getSuffix() {
        return suffix;
    }

    public static Optional<Sample> getSample(char suffix) {
        for (Sample sample : Sample.values()) {
            if (sample.getSuffix() == suffix) {
                return Optional.of(sample);
            }
        }
        return Optional.absent();
    }
}
