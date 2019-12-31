package io.github.trho.rxstacktrace;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Function;
import org.reactivestreams.Publisher;

public final class RxStacktraceFlowable<T> extends RxStracktrace implements FlowableTransformer<T,T> {


    public RxStacktraceFlowable(String signature) {
        super(signature);
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return upstream.onErrorResumeNext((Function<Throwable, Publisher<T>>) throwable -> {

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

