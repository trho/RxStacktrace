package io.github.trho.rxstacktrace;

import de.icongmbh.oss.maven.plugin.javassist.ClassTransformer;
import io.reactivex.*;
import io.reactivex.Observable;
import javassist.*;
import javassist.build.JavassistBuildException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.*;

public class RxAsyncStacktraceTransformer extends ClassTransformer {

    private static final String INCLUDE_SYNC_CALLS_PROPERTY_KEY = "include.sync.calls";
    private static final String INCLUDE_PACKAGES_WITH_PREFIXES_PROPERTY_KEY = "prefixes.included.packages";
    private static final String EXCLUDE_CLASSES_PROPERTY_KEY = "exclude.classes";

    private static final String STACK_COLLECTOR_CLASS_PROPERTY_KEY = "stack.collector.class";
    private static final String STACK_COLLECTOR_PARAM_NAME_PROPERTY_KEY = "stack.collector.param.name";

    private boolean includeSyncCalls = false;

    private String[] includePackagePrefixes;
    private Set<String> excludeClasses;

    private String stackCollectorClassName = null;
    private String stackCollectorParamName;

    private int localInstrumentCounter = 0;
    private int instrumentCounter = 0;

    private Class<?>[] classes = {Flowable.class, Maybe.class, Observable.class, Single.class, Completable.class};
    private Set<String> rxClassNames = new HashSet<>();

    public RxAsyncStacktraceTransformer() {

        rxClassNames.add(Single.class.getName());
        rxClassNames.add(Completable.class.getName());
        rxClassNames.add(Observable.class.getName());
        rxClassNames.add(Maybe.class.getName());
        rxClassNames.add(Flowable.class.getName());
    }


    /**
     * We'll only transform subtypes of MyInterface.
     */
    @Override
    public boolean shouldTransform(final CtClass candidateClass) {
        return checkPackage(candidateClass.getPackageName()) &&
                !excludeClasses.contains(candidateClass.getName());
    }

    private boolean checkPackage(String packageName) {
        for (String includePackagePrefix : includePackagePrefixes) {
            if (packageName.startsWith(includePackagePrefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hack the toString() method.
     */
    @Override
    public void applyTransformations(CtClass cc)
            throws JavassistBuildException {
        try {

            localInstrumentCounter = 0;
            CtMethod[] methods = cc.getDeclaredMethods();

            final String className = cc.getName();
            for (final CtMethod cm : methods) {
                String paramName = getStackTraceCollectorParameter(cm);

                cm.instrument(
                        new ExprEditor() {
                            public void edit(MethodCall mc)
                                    throws CannotCompileException {
                                try {

                                    boolean hasRxReturnType = isRxReturnType(mc);

                                    boolean calledMethodHasCollectorParam = false;

                                    if (includeSyncCalls && !hasRxReturnType) {
                                        calledMethodHasCollectorParam = getStackTraceCollectorParameter(mc.getMethod()) != null;
                                    }

                                    if (hasRxReturnType || calledMethodHasCollectorParam) {
                                        String signature = RxStacktraceHelper.getSignature(mc, cc, cm);
                                        if (calledMethodHasCollectorParam) {
                                            signature += " - sync";
                                        }
                                        if (paramName != null) {
                                            // FIXME: name is sometimes incorrectly resolved,
                                            // therefore stackCollectorParamName parameter name is forced:
                                            String overrideParamName = stackCollectorParamName;
                                            instrumentCounter++;
                                            localInstrumentCounter++;
                                            mc.replace(overrideParamName + ".addStacktraceLine(\""
                                                    + signature + "\"); $_ = $proceed($$);");
                                        } else {
                                            if (hasRxReturnType) {
                                                wrapTypes(classes, mc, cc, cm);
                                            }
                                        }
                                    }
                                } catch (NotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
            }

            if (localInstrumentCounter > 0) {
                System.err.println(className + " instrumented: " + instrumentCounter + ", " + localInstrumentCounter);
            }

            for (CtClass nestedClass: cc.getNestedClasses()) {
                nestedClass.defrost();
                applyTransformations(nestedClass);
            }

        } catch (Exception e) {
            System.err.println("ERROR: " + cc.getName());
            throw new JavassistBuildException(e);
        }
    }

    private boolean isRxReturnType(MethodCall mc) throws NotFoundException {
        String returnType = mc.getMethod().getReturnType().getName();
        return rxClassNames.contains(returnType);
    }

    private String getStackTraceCollectorParameter(CtMethod cm) throws NotFoundException {
        for (int i = 0; i < cm.getParameterTypes().length; i++) {
            CtClass cc = cm.getParameterTypes()[i];
            if (cc.getName().equals(stackCollectorClassName)) {

                MethodInfo methodInfo = cm.getMethodInfo2();
                CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
                if (codeAttribute == null) { // TODO: check
                    return stackCollectorParamName;
                }
                LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
                int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
                return attr.variableName(i + pos);
            }
        }
        return null;
    }

    private boolean wrapTypes(Class<?>[] classes, MethodCall m, CtClass cc, CtMethod cm) throws CannotCompileException {
        try {
            for (Class<?> clazz : classes) {
                if (m.getMethod().getReturnType().getName().equals(clazz.getName())) {
                    String signature = RxStacktraceHelper.getSignature(m, cc, cm);
                    instrumentCounter++;
                    localInstrumentCounter++;
                    m.replace(
                            "$_ = io.github.trho.rxstacktrace.RxStacktraceHelper.wrap" +
                                    clazz.getSimpleName() + "($proceed($$), \""
                                    + signature + "\");");
                    break;
                }
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public void configure(final Properties properties) {
        if (null == properties) {
            return;
        }
        includeSyncCalls = Boolean.parseBoolean(properties.getProperty(INCLUDE_SYNC_CALLS_PROPERTY_KEY, "false"));

        includePackagePrefixes = toStringArray(properties.getProperty(INCLUDE_PACKAGES_WITH_PREFIXES_PROPERTY_KEY));
        if (includePackagePrefixes == null) {
            getLogger().error("missing property: " + INCLUDE_PACKAGES_WITH_PREFIXES_PROPERTY_KEY);
        }
        String[] excludeClassesArray = toStringArray(properties.getProperty(EXCLUDE_CLASSES_PROPERTY_KEY));
        if (excludeClassesArray == null) {
            this.excludeClasses = new HashSet<>();
        } else {
            this.excludeClasses = new HashSet<>(Arrays.asList(excludeClassesArray));
        }

        stackCollectorClassName = properties.getProperty(STACK_COLLECTOR_CLASS_PROPERTY_KEY);
        if (stackCollectorClassName != null) {
            try {
                Class<?> stackCollectorClass = Class.forName(stackCollectorClassName);
                stackCollectorParamName = properties.getProperty(STACK_COLLECTOR_PARAM_NAME_PROPERTY_KEY);
                getLogger().info("stacktrace collector class {}", stackCollectorClassName);
                getLogger().info("stacktrace collector parameter name {}", stackCollectorParamName);
            } catch (ClassNotFoundException e) {
                getLogger().error("loading stack holder class failed");
                e.printStackTrace();
            }
        }
    }

    private String[] toStringArray(String property) {
        if (property == null) {
            return null;
        }
        return property.split(",");

    }
}