package com.github.hammelion.resources;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.hammelion.annotations.RAMLConfig;
import com.github.hammelion.resources.model.TestId;

@RAMLConfig("api.raml")
public class TestResource {

    public TestSubResource testId(TestId id) throws NoSuchMethodException {
        return new TestSubResource(id);

    }

    public List<Annotation> postById(TestId id) throws NoSuchMethodException {
        return methodAnnotations("postById", new Class[] { TestId.class });
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
