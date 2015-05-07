package org.jraml.resources;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jraml.RAMLConfig;
import org.jraml.resources.model.TestId;

public class TestSubResource {

    private final TestId id;

    public TestSubResource(TestId id) {

        this.id = id;
    }

    public List<Annotation> getById() throws NoSuchMethodException {
        return methodAnnotations("getById", new Class[] { });

    }

    private List<Annotation> methodAnnotations(String methodName, Class[] parameterTypes) throws NoSuchMethodException {
        final List<Annotation> annotations = new ArrayList<>();
        annotations.addAll(Arrays.asList(getClass().getMethod(methodName, parameterTypes).getAnnotations()));
        for (Annotation[] parameterAnnotations : parametersAnnotations(methodName, parameterTypes)) {
            annotations.addAll(Arrays.asList(parameterAnnotations));
        }
        return annotations;
    }

    private Annotation[][] parametersAnnotations(String methodName, Class[] parameterTypes) throws NoSuchMethodException {
        return getClass().getMethod(methodName, parameterTypes).getParameterAnnotations();
    }
}
