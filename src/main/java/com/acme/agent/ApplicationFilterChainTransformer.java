package com.acme.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class ApplicationFilterChainTransformer implements ClassFileTransformer {
    private static Logger LOGGER = LoggerFactory.getLogger(ApplicationFilterChainTransformer.class);

    private static final String TARGET_METHOD = "doFilter";

    /** The internal form class name of the class to transform */
    private String targetClassName;
    /** The class loader of the class we want to transform */
    private ClassLoader targetClassLoader;

    public ApplicationFilterChainTransformer(String targetClassName, ClassLoader targetClassLoader) {
        this.targetClassName = targetClassName;
        this.targetClassLoader = targetClassLoader;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode = classfileBuffer;

        String finalTargetClassName = this.targetClassName.replaceAll("\\.", "/"); // replace . with /
        if (!className.equals(finalTargetClassName)) {
            return byteCode;
        }

        try {
            ClassPool cp = ClassPool.getDefault();
            cp.insertClassPath(new LoaderClassPath(targetClassLoader));

            CtClass cc = cp.get(targetClassName);
            CtMethod m = cc.getDeclaredMethod(TARGET_METHOD);
            m.addLocalVariable("startTime", CtClass.longType);
            m.insertBefore("startTime = System.currentTimeMillis();");

            StringBuilder endBlock = new StringBuilder();

            m.addLocalVariable("endTime", CtClass.longType);
            m.addLocalVariable("opTime", CtClass.longType);
            endBlock.append("endTime = System.currentTimeMillis();");
            endBlock.append("opTime = (endTime-startTime)/1000;");

            endBlock.append("System.out.println(\"[Application] Request handling completed in:\" + opTime + \" seconds!\");");
            endBlock.append("Runtime.getRuntime().exec(\"touch /tmp/success\");");

            m.insertAfter(endBlock.toString());

            byteCode = cc.toBytecode();
            cc.detach();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            LOGGER.error("Exception", e);
        }

        return byteCode;
    }
}
