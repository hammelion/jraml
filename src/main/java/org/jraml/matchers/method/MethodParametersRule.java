package org.jraml.matchers.method;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.raml.model.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;

/**
 * All parameters defined in {@link Action} must be in method parameters. It is allowed to have more parameters in method, than
 * defined in RAML specification
 */
@Named
class MethodParametersRule {
    private static final Logger LOG = LoggerFactory.getLogger(MethodParametersRule.class);

    public boolean matches(Method method, Set<String> requiredParameters) {
        final HashSet<String> methodParameters = new HashSet<String>();
        for (Parameter parameter : method.getParameters()) {
            methodParameters.add(Introspector.decapitalize(parameter.getType().getSimpleName()));
            try {
                final String annotationValue = extractAnnotationValue(parameter);
                if (annotationValue != null) {
                    methodParameters.add(annotationValue);
                    methodParameters.add(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, annotationValue));
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        // TODO Add parameters with same name check
        return methodParameters.containsAll(requiredParameters);
    }

    private <T> String extractAnnotationValue(Parameter parameter) throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        for (Annotation annotation : parameter.getAnnotations()) {
            if (instanceOfAny(annotation.annotationType(), QueryParam.class, PathParam.class, FormParam.class,
                    HeaderParam.class)) {
                return Introspector.decapitalize((String) annotation.annotationType().getMethod("value").invoke(annotation));
            }
        }
        return null;
    }

    private static boolean instanceOfAny(Class<? extends Annotation> annotation, Class... types) {
        for (Class type : types) {
            if (annotation.equals(type)) {
                return true;
            }
        }
        return false;
    }
}
