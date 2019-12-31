package io.github.trho.rxstacktrace;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Function;

public final class RxStacktraceSingle<T> extends RxStracktrace implements SingleTransformer<T,T> {


    public RxStacktraceSingle(String signature) {
        super(signature);
    }

    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        return upstream.onErrorResumeNext((Function<Throwable, SingleSource<T>>) throwable -> {
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

