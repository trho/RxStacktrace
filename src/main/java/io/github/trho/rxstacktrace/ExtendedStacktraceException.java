package io.github.trho.rxstacktrace;

import java.util.ArrayList;
import java.util.List;

public class ExtendedStacktraceException extends Exception {

    public List<String> entries = new ArrayList<>();

    public ExtendedStacktraceException(Throwable throwable) {
        super("Extended stack trace", throwable, true, false);

    }

    @Override
    public String getMessage() {
        return "Extended stack trace: \n" + String.join( "\n", entries.toArray(new String[0]));
    }

}
