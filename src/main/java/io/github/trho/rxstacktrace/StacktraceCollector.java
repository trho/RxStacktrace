package io.github.trho.rxstacktrace;

import java.util.List;

/**
 * 
 */
public interface StacktraceCollector {
    void addStacktraceLine(String signature);

    List<String> getStacktraceElements();
}
