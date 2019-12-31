package io.github.trho.rxstacktrace;

import io.reactivex.*;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.MethodCall;

public class RxStacktraceHelper {
    public static String getSignature(MethodCall mc, CtClass cc, CtMethod cm) throws NotFoundException {
        return mc.getEnclosingClass().getName() + "." + cm.getName() + "(" + mc.getFileName() + ":" + mc.getLineNumber() + ") - " + mc.getMethod().getDeclaringClass().getSimpleName()  + "." + mc.getMethodName()+ "()";
    }

    public static String getSignature(CtClass cc, CtMethod cm) {
        return cc.getName() + "." + cm.getName() + "(" + cc.getClassFile().getSourceFile() + ":" + cm.getMethodInfo().getLineNumber(0) + ")";
    }

    public static <T> Single<T> wrapSingle(Single<T> s, String signature) {
        return s.compose(new RxStacktraceSingle<>(signature));
    }

    public static <T> Observable<T> wrapObservable(Observable<T> s, String signature) {
        return s.compose(new RxStacktraceObservable<>(signature));
    }

    public static Completable wrapCompletable(Completable s, String signature) {
        return s.compose(new RxStacktraceCompletable(signature));
    }

    public static <T> Maybe<T> wrapMaybe(Maybe<T> s, String signature) {
        return s.compose(new RxStacktraceMaybe<>(signature));
    }

    public static <T> Flowable<T> wrapFlowable(Flowable<T> s, String signature) {
        return s.compose(new RxStacktraceFlowable<>(signature));
    }

}
