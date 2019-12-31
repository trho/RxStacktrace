package io.github.trho.rxstacktrace;

public class RxStracktrace {

    protected final String signature;

    public RxStracktrace(String signature) {
        this.signature = signature;
    }

    protected String toSignatureTrace() {
        return signature;
    }
}

