package com.github.hammelion.processors;

import java.lang.reflect.Method;
import java.text.MessageFormat;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Path;

import org.raml.model.Resource;

import com.github.hammelion.annotations.RAMLConfig;
import com.github.hammelion.exceptions.CorrespondingMethodNotFoundException;
import com.github.hammelion.matchers.method.MethodMatcher;
import com.github.hammelion.parsers.RAMLParserFacade;
import com.github.hammelion.utils.javassist.Methods;

@Named
public class ResourceProcessor {
    private final ClassPool POOL = ClassPool.getDefault();

    private final MethodMatcher methodMatcher;

    private final ActionProcessor actionProcessor;

    private final RAMLParserFacade ramlParserFacade;

    @Inject
    public ResourceProcessor(MethodMatcher methodMatcher, ActionProcessor actionProcessor, RAMLParserFacade ramlParserFacade) {
        this.methodMatcher = methodMatcher;
        this.actionProcessor = actionProcessor;
        this.ramlParserFacade = ramlParserFacade;
    }

    public void process(Class<?> originalClass, ClassLoader classLoader)
            throws NotFoundException, CorrespondingMethodNotFoundException, CannotCompileException {
        final String ramlFilePath = originalClass.getAnnotation(RAMLConfig.class).value();
        final String resourceKey = determineResourceKey(originalClass);
        final Resource resource = ramlParserFacade.findResource(ramlFilePath, resourceKey);
        process(resource, originalClass, classLoader);
    }

    private void process(Resource resource, Class<?> originalClass, ClassLoader classLoader) throws NotFoundException,
            CorrespondingMethodNotFoundException, CannotCompileException {
        if (resource != null) {
            final CtClass clazz = POOL.getCtClass(originalClass.getName());
            final ConstPool constPool = clazz.getClassFile().getConstPool();
            this.actionProcessor.process(resource, constPool, clazz, originalClass);
            for (Resource subResource : resource.getResources().values()) {
                processSubResource(classLoader, clazz, constPool, originalClass, subResource);
            }
            clazz.toClass(classLoader);
        }
    }

    private void processSubResource(ClassLoader classLoader, CtClass clazz, ConstPool constPool, Class<?> originalClass,
            Resource subResource) throws NotFoundException, CorrespondingMethodNotFoundException, CannotCompileException {
        for (Method originalMethod : originalClass.getMethods()) {
            if (this.methodMatcher.matches(originalMethod, subResource)) {
                final Path path = originalMethod.getAnnotation(Path.class);
                if (path == null) {
                    final CtMethod method = clazz.getDeclaredMethod(originalMethod.getName());
                    Methods.annotateMethod(constPool, method, originalMethod, POOL.getCtClass(Path.class.getName()));
                }
                process(subResource, originalMethod.getReturnType(), classLoader);
                return;
            }
        }
        throw new CorrespondingMethodNotFoundException(MessageFormat.format(
                "Method''s name should start with ''{0}'' and have no parameters",
                subResource.getRelativeUri().replaceAll("[\\{\\}/]", "")));
    }

    private String determineResourceKey(Class<?> originalClass) {
        final Path path = originalClass.getAnnotation(Path.class);
        if (path != null) {
            return path.value().toLowerCase();
        } else {
            return "/" + originalClass.getSimpleName().toLowerCase().replace("resource", "");
        }
    }

}
