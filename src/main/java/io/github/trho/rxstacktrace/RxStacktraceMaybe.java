package io.github.trho.rxstacktrace;

import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.MaybeTransformer;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Function;

public final class RxStacktraceMaybe<T> extends RxStracktrace implements MaybeTransformer<T,T> {


    public RxStacktraceMaybe(String signature) {
        super(signature);
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        return upstream.onErrorResumeNext((Function<Throwable, MaybeSource<T>>) throwable -> {

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

