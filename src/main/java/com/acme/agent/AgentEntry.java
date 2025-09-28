package com.acme.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

public class AgentEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentEntry.class);
    private static final String TARGET_CLASS = "org.apache.catalina.core.ApplicationFilterChain";

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In premain");
        installAndRetransformIfLoaded(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In agentmain");
        installAndRetransformIfLoaded(inst);
    }

    private static void installAndRetransformIfLoaded(Instrumentation inst) {
        Class<?> targetCls = null;
        ClassLoader targetClassLoader = null;
        for(Class<?> clazz: inst.getAllLoadedClasses()) {
            if(clazz.getName().equals(TARGET_CLASS)) {
                targetCls = clazz;
                targetClassLoader = targetCls.getClassLoader();
                transform(targetCls, targetClassLoader, inst);
                return;
            }
        }
    }

    private static void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation inst) {
        ApplicationFilterChainTransformer dt = new ApplicationFilterChainTransformer(clazz.getName(), classLoader);
        inst.addTransformer(dt, true);
        try {
            inst.retransformClasses(clazz);
        } catch (Exception ex) {
            throw new RuntimeException("Transform failed for class: [" + clazz.getName() + "]", ex);
        }
    }
}