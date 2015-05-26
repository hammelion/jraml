package com.github.hammelion.processors;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.github.hammelion.RAMLConfig;
import com.github.hammelion.exceptions.CorrespondingMethodNotFoundException;
import com.github.hammelion.matchers.method.MethodMatcher;
import com.github.hammelion.parsers.RAMLParserFacade;
import org.raml.model.Action;
import org.raml.model.Resource;
import org.raml.model.parameter.UriParameter;

import com.google.common.collect.Sets;

@Named
public class ResourceProcessor {
    private final ClassPool POOL = ClassPool.getDefault();

    private final MethodMatcher methodMatcher;

    @Inject
    public ResourceProcessor(MethodMatcher methodMatcher) {
        this.methodMatcher = methodMatcher;
    }

    public void process(RAMLParserFacade ramlParserFacade, Class<?> originalClass, ClassLoader classLoader)
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
            Set<String> missingUriParameters = determineMissingUriParameters(originalClass, resource.getUriParameters());
            actionLoop: for (Action action : resource.getActions().values()) {
                processAction(clazz, constPool, originalClass, missingUriParameters, action);
            }
            resourceLoop: for (Resource subResource : resource.getResources().values()) {
                processSubResource(classLoader, clazz, constPool, originalClass, subResource);
            }
            clazz.toClass(classLoader);
        }
    }

    private Set<String> determineMissingUriParameters(Class<?> originalClass, Map<String, UriParameter> uriParameters) {
        Set<String> missingUriParameters = new HashSet<>();
        Set<String> fields = new HashSet<>();
        for (Field field : originalClass.getDeclaredFields()) {
            fields.add(Introspector.decapitalize(field.getType().getSimpleName()));
        }
        if (uriParameters != null) {
            Sets.difference(uriParameters.keySet(), fields).copyInto(missingUriParameters);

        }
        return missingUriParameters;
    }

    private void processSubResource(ClassLoader classLoader, CtClass clazz, ConstPool constPool, Class<?> originalClass,
            Resource subResource) throws NotFoundException, CorrespondingMethodNotFoundException, CannotCompileException {
        for (Method originalMethod : originalClass.getMethods()) {
            if (this.methodMatcher.matches(originalMethod, subResource)) {
                final Path path = originalMethod.getAnnotation(Path.class);
                if (path == null) {
                    final CtMethod method = clazz.getDeclaredMethod(originalMethod.getName());
                    annotateMethod(constPool, method, originalMethod, Path.class.getName());
                }
                process(subResource, originalMethod.getReturnType(), classLoader);
                return;
            }
        }
        throw new CorrespondingMethodNotFoundException(MessageFormat.format(
                "Method''s name should start with ''{0}'' and have no parameters",
                subResource.getRelativeUri().replaceAll("[\\{\\}/]", "")));
    }

    private void processAction(CtClass clazz, ConstPool constPool, Class<?> originalClass, Set<String> missingUriParameters,
            Action action) throws NotFoundException, CorrespondingMethodNotFoundException {
        for (Method originalMethod : originalClass.getMethods()) {
            if (this.methodMatcher.matches(originalMethod, missingUriParameters, action)) {
                final CtMethod method = clazz.getDeclaredMethod(originalMethod.getName());
                annotateMethod(constPool, method, originalMethod, "javax.ws.rs." + action.getType().name());
                annotateMethodParameters(constPool, method, originalMethod, action, missingUriParameters);
                return;
            }
        }
        throw new CorrespondingMethodNotFoundException(MessageFormat.format(
                "Method''s name should start with ''{0}'' and have following parameters:\n{1} {2}", action.getType().toString()
                        .toLowerCase(), action.getBaseUriParameters(), action.getQueryParameters()));
    }

    private void annotateSubResourceMethod(CtClass clazz, Method originalMethod, Resource subResource) throws NotFoundException {

    }

    private String determineResourceKey(Class<?> originalClass) {
        final Path path = originalClass.getAnnotation(Path.class);
        if (path != null) {
            return path.value().toLowerCase();
        } else {
            return "/" + originalClass.getSimpleName().toLowerCase().replace("resource", "");
        }
    }

    private void annotateMethod(ConstPool constPool, CtMethod method, Method originalMethod, String annotationName)
            throws NotFoundException {
        Annotation annotation = new Annotation(constPool, POOL.getCtClass(annotationName));
        AnnotationsAttribute attribute = (AnnotationsAttribute) method.getMethodInfo().getAttribute(
                AnnotationsAttribute.visibleTag);
        if (attribute == null) {
            attribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            method.getMethodInfo().addAttribute(attribute);
        }
        attribute.addAnnotation(annotation);
    }

    private void annotateMethodParameters(ConstPool constPool, CtMethod method, Method originalMethod, Action action,
            Set<String> missingUriParameters) throws NotFoundException {
        if (action != null) {
            ParameterAnnotationsAttribute attribute = ((ParameterAnnotationsAttribute) method.getMethodInfo().getAttribute(
                    ParameterAnnotationsAttribute.visibleTag));
            if (attribute == null) {
                attribute = new ParameterAnnotationsAttribute(constPool, ParameterAnnotationsAttribute.visibleTag);
                method.getMethodInfo().addAttribute(attribute);
            }
            Annotation[][] allAnnotations = attribute.getAnnotations();
            if (allAnnotations.length == 0) {
                allAnnotations = new Annotation[originalMethod.getParameters().length][];
            }
            annotateMethodParameters(constPool, attribute, allAnnotations, originalMethod, action.getQueryParameters().keySet(),
                    QueryParam.class);
            annotateMethodParameters(constPool, attribute, allAnnotations, originalMethod, missingUriParameters, PathParam.class);
            if (action.getBody() != null) {
                annotateMethodParameters(constPool, attribute, allAnnotations, originalMethod, action.getBody().keySet(),
                        FormParam.class);
            }
            annotateMethodParameters(constPool, attribute, allAnnotations, originalMethod, action.getHeaders().keySet(),
                    HeaderParam.class);
            attribute.setAnnotations(allAnnotations);
        }
    }

    private void annotateMethodParameters(final ConstPool constPool, final ParameterAnnotationsAttribute attribute,
            final Annotation[][] allAnnotations, final Method originalMethod, final Set<String> requiredParameters,
            final Class<? extends java.lang.annotation.Annotation> annotationType) throws NotFoundException {

        for (int parameterPosition = 0; parameterPosition < originalMethod.getParameters().length; parameterPosition++) {
            Parameter parameter = originalMethod.getParameters()[parameterPosition];
            final String restParameter = findCorrespondingRestParameter(parameter, requiredParameters);
            if (!parameterIsAnnotated(parameter, annotationType) && restParameter != null) {
                Annotation annotation = new Annotation(constPool, POOL.getCtClass(annotationType.getName()));
                annotation.addMemberValue("value", new StringMemberValue(restParameter, attribute.getConstPool()));
                final Annotation[] annotations = allAnnotations[parameterPosition];
                final List<Annotation> annotationList;
                if (annotations == null) {
                    annotationList = new ArrayList<>();
                } else {
                    annotationList = Arrays.asList(annotations);
                }
                annotationList.add(annotation);
                allAnnotations[parameterPosition] = annotationList.toArray(new Annotation[annotationList.size()]);
            }

        }
        // TODO DefaultValue
    }

    private String findCorrespondingRestParameter(Parameter parameter, Set<String> requiredParameters) {
        final String parameterName = parameter.getType().getSimpleName();
        for (String requiredParameter : requiredParameters) {
            if (parameterName.equalsIgnoreCase(requiredParameter)) {
                return requiredParameter;
            }
        }
        return null;
    }

    private boolean parameterIsAnnotated(Parameter parameter, Class<? extends java.lang.annotation.Annotation> requiredType) {
        for (java.lang.annotation.Annotation annotation : parameter.getAnnotations()) {
            if (annotation.annotationType().equals(requiredType)) {
                return true;
            }
        }
        return false;
    }

}
