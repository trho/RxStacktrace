package io.github.trho.rxstacktrace;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.CompletableTransformer;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Function;

public final class RxStacktraceCompletable extends RxStracktrace implements CompletableTransformer {

    public RxStacktraceCompletable(String sig) {
        super(sig);
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        return upstream.onErrorResumeNext(throwable -> {
            if(throwable instanceof CompositeException
                    && ((CompositeException) throwable).getExceptions().size() == 2
                    && ((CompositeException) throwable).getExceptions().get(1) instanceof ExtendedStacktraceException ) {
                ((ExtendedStacktraceException) ((CompositeException) throwable).getExceptions().get(1)).entries.add(toSignatureTrace());
                throw (Exception)throwable;
            } else {
                ExtendedStacktraceException extException = new ExtendedStacktraceException(null);
                extException.entries.add(toSignatureTrace());
                throw new CompositeException(throwable, extException);
            }
        });
    }
}
