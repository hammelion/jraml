package com.github.hammelion.utils.javassist;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * TODO JavaDoc in com.github.hammelion.utils.javassist
 */
public abstract class Methods {
    public static void annotateMethod(ConstPool constPool, CtMethod method, Method originalMethod, CtClass annotationClass)
            throws NotFoundException {
        annotateMethod(constPool, method, originalMethod, annotationClass, null);
    }

    public static void annotateMethod(ConstPool constPool, CtMethod method, Method originalMethod, CtClass annotationClass,
            ArrayMemberValue annotationValue) throws NotFoundException {
        Annotation annotation = new Annotation(constPool, annotationClass);
        if (annotationValue != null) {
            annotation.addMemberValue("value", annotationValue);
        }
        AnnotationsAttribute attribute = (AnnotationsAttribute) method.getMethodInfo().getAttribute(
                AnnotationsAttribute.visibleTag);
        if (attribute == null) {
            attribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            method.getMethodInfo().addAttribute(attribute);
        }
        attribute.addAnnotation(annotation);
    }

    public static void annotateMethodParameters(final ConstPool constPool, final ParameterAnnotationsAttribute attribute,
            final Annotation[][] allAnnotations, final Method originalMethod, final Set<String> requiredParameters,
            final Class<? extends java.lang.annotation.Annotation> annotationType, final CtClass annotationClass)
            throws NotFoundException {

        for (int parameterPosition = 0; parameterPosition < originalMethod.getParameters().length; parameterPosition++) {
            Parameter parameter = originalMethod.getParameters()[parameterPosition];
            final String restParameter = findCorrespondingRestParameter(parameter, requiredParameters);
            if (!parameterIsAnnotated(parameter, annotationType) && restParameter != null) {
                Annotation annotation = new Annotation(constPool, annotationClass);
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

    private static String findCorrespondingRestParameter(Parameter parameter, Set<String> requiredParameters) {
        final String parameterName = parameter.getType().getSimpleName();
        for (String requiredParameter : requiredParameters) {
            if (parameterName.equalsIgnoreCase(requiredParameter)) {
                return requiredParameter;
            }
        }
        return null;
    }

    private static boolean parameterIsAnnotated(Parameter parameter, Class<? extends java.lang.annotation.Annotation> requiredType) {
        for (java.lang.annotation.Annotation annotation : parameter.getAnnotations()) {
            if (annotation.annotationType().equals(requiredType)) {
                return true;
            }
        }
        return false;
    }
}
