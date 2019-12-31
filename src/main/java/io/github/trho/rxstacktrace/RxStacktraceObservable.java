package io.github.trho.rxstacktrace;

import io.reactivex.*;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Function;

public final class RxStacktraceObservable<T> extends RxStracktrace implements ObservableTransformer<T,T> {


    public RxStacktraceObservable(String signature) {
        super(signature);
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return upstream.onErrorResumeNext((Function<Throwable, ObservableSource<? extends T>>) throwable -> {
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

